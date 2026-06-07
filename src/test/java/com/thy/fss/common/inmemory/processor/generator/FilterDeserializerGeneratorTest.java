package com.thy.fss.common.inmemory.processor.generator;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.thy.fss.common.inmemory.filter.FilterConstants;
import com.thy.fss.common.inmemory.processor.exception.DeserializerGenerationException;
import com.thy.fss.common.inmemory.processor.model.AnnotationInfo;
import com.thy.fss.common.inmemory.processor.model.DateTimeFormatInfo;
import com.thy.fss.common.inmemory.processor.model.EnumDeserializationInfo;
import com.thy.fss.common.inmemory.processor.model.FilterFieldConfig;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Unit tests for FilterDeserializerGenerator unified infrastructure.
 * Tests the common deserializer generation logic, streaming JSON parser templates,
 * and performance optimizations.
 */
@DisplayName("FilterDeserializerGenerator Tests")
class FilterDeserializerGeneratorTest {

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
    void setUp()  {
        MockitoAnnotations.openMocks(this);
        when(processingEnv.getFiler()).thenReturn(filer);
        when(processingEnv.getMessager()).thenReturn(messager);
        when(processingEnv.getElementUtils()).thenReturn(elements);
        when(processingEnv.getTypeUtils()).thenReturn(types);

        generator = new FilterDeserializerGenerator(processingEnv);
    }

    private static final String CREATED_DATE_FIELD = "createdDate";
    private static final String LOCAL_DATE_TIME_FILTER = "LocalDateTimeFilter";
    private static final String COM_TEST_STATUS = "com.test.Status";
    private static final String STATUS_FIELD = "status";
    private static final String ENUM_FILTER = "EnumFilter";
    private static final String RETURN_ENUM_VALUE = "return enumValue;";
    private static final String COM_EXAMPLE_PACKAGE = "com.example";
    private static final String PACKAGE_COM_EXAMPLE = "package com.example;";
    private static final String ITEMS_FIELD = "items";
    private static final String JAVA_UTIL_COLLECTION = "java.util.Collection";
    private static final String COLLECTION_FILTER = "CollectionFilter";
    private static final String TESTMODEL_PACKAGE = "com.thy.fss.common.inmemory.testmodel";

    @Test
    @DisplayName("Should generate basic deserializer structure")
    void shouldGenerateBasicDeserializerStructure() throws Exception {
        // Given
        FilterFieldConfig stringConfig = new FilterFieldConfig("name", "java.lang.String", "StringFilter");
        stringConfig.setString(true);
        List<FilterFieldConfig> fieldConfigs = List.of(stringConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("TestFilter", "com.test", fieldConfigs, "TestEntity");

        // Then
        String generatedCode = stringWriter.toString();

        assertThat(generatedCode).contains("package com.test;")
                .contains("public class TestFilterDeserializer extends " + JsonDeserializer.class.getSimpleName() + "<TestFilter>")
                .contains("public TestFilter deserialize(" + JsonParser.class.getSimpleName() + " p, " + DeserializationContext.class.getSimpleName() + " ctxt)")
                .contains("Performance: Direct object creation, no reflection")
                .contains("Performance: Streaming parser, no intermediate tree")
                .contains("Performance: Switch statement with interned strings");
    }

    @Test
    @DisplayName("Should include common imports")
    void shouldIncludeCommonImports() throws Exception {
        // Given
        FilterFieldConfig stringConfig = new FilterFieldConfig("name", "java.lang.String", "StringFilter");
        stringConfig.setString(true);
        List<FilterFieldConfig> fieldConfigs = List.of(stringConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("TestFilter", "com.test", fieldConfigs, "TestEntity");

        // Then
        String generatedCode = stringWriter.toString();

        assertThat(generatedCode).contains("import " + JsonParser.class.getName() + ";")
                .contains("import " + JsonToken.class.getName() + ";")
                .contains("import " + DeserializationContext.class.getName() + ";")
                .contains("import " + JsonDeserializer.class.getName() + ";")
                .contains("import " + IOException.class.getName() + ";")
                .contains("import " + ArrayList.class.getName() + ";")
                .contains("import " + List.class.getName() + ";")
                .contains("import " + DateTimeFormatter.class.getName() + ";")
                .contains("import " + FilterConstants.class.getName() + ";");
    }

    @Test
    @DisplayName("Should handle temporal field configuration")
    void shouldHandleTemporalFieldConfiguration() {
        // Given
        DateTimeFormatInfo formatInfo = DateTimeFormatInfo.withCustomPattern("yyyy-MM-dd HH:mm:ss", LocalDateTime.class.getName());
        FilterFieldConfig temporalConfig = new FilterFieldConfig(CREATED_DATE_FIELD,
                LocalDateTime.class.getName(), LOCAL_DATE_TIME_FILTER);
        temporalConfig.setTemporal(true);
        temporalConfig.setDateTimeFormatInfo(formatInfo);

        // When & Then
        assertThat(temporalConfig.isTemporal()).isTrue();
        assertThat(temporalConfig.hasCustomDateTimeFormat()).isTrue();
        assertThat(temporalConfig.getEffectiveDateTimePattern()).isEqualTo("yyyy-MM-dd HH:mm:ss");
    }

    @Test
    @DisplayName("Should handle enum field configuration")
    void shouldHandleEnumFieldConfiguration() {
        // Given
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo(COM_TEST_STATUS);
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.CREATOR_METHOD);
        enumInfo.setJsonCreatorMethod("fromValue");

        FilterFieldConfig enumConfig = new FilterFieldConfig(STATUS_FIELD, COM_TEST_STATUS, "StatusFilter");
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);

        // When & Then
        assertThat(enumConfig.isEnum()).isTrue();
        assertThat(enumConfig.hasCustomEnumDeserialization()).isTrue();
        assertThat(enumConfig.getEnumDeserializationType()).isEqualTo(EnumDeserializationInfo.DeserializationType.CREATOR_METHOD);
    }

    @Test
    @DisplayName("Should generate LocalDateTimeFilter deserializer with custom format")
    void shouldGenerateLocalDateTimeFilterDeserializerWithCustomFormat() throws Exception {
        // Given
        DateTimeFormatInfo formatInfo = DateTimeFormatInfo.withCustomPattern("yyyy-MM-dd HH:mm:ss", LocalDateTime.class.getName());
        FilterFieldConfig temporalConfig = new FilterFieldConfig(CREATED_DATE_FIELD,
                LocalDateTime.class.getName(), LOCAL_DATE_TIME_FILTER);
        temporalConfig.setTemporal(true);
        temporalConfig.setDateTimeFormatInfo(formatInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(temporalConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(LOCAL_DATE_TIME_FILTER, "com.test", fieldConfigs, "TemporalEntity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify package and class structure
        assertThat(generatedCode).contains("package com.test;")
                .contains("public class LocalDateTimeFilterDeserializer extends " + JsonDeserializer.class.getSimpleName() + "<LocalDateTimeFilter>")
                .contains("import " + LocalDateTime.class.getName() + ";")
                .contains("import " + DateTimeFormatter.class.getName() + ";")
                .contains("private static final " + DateTimeFormatter.class.getSimpleName() + " FORMATTER_CREATEDDATE")
                .contains("DateTimeFormatter.ofPattern(\"yyyy-MM-dd HH:mm:ss\")")
                .contains("case \"createdDate\"")
                .contains(CREATED_DATE_FIELD)
                .contains("private static " + LocalDateTime.class.getSimpleName() + " parseLocalDateTime_createdDate(String value)")
                .contains("LocalDateTime.parse(value, FORMATTER_CREATEDDATE)");
    }

    @Test
    @DisplayName("Should generate LocalDateTimeFilter deserializer with default format")
    void shouldGenerateLocalDateTimeFilterDeserializerWithDefaultFormat() throws Exception {
        // Given
        DateTimeFormatInfo formatInfo = DateTimeFormatInfo.withDefaultPattern(LocalDateTime.class.getName());
        FilterFieldConfig temporalConfig = new FilterFieldConfig("updatedDate",
                LocalDateTime.class.getName(), LOCAL_DATE_TIME_FILTER);
        temporalConfig.setTemporal(true);
        temporalConfig.setDateTimeFormatInfo(formatInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(temporalConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(LOCAL_DATE_TIME_FILTER, "com.test", fieldConfigs, "TemporalEntity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify default formatter generation
        assertThat(generatedCode).contains("private static final " + DateTimeFormatter.class.getSimpleName() + " FORMATTER_UPDATEDDATE")
                .contains(FilterConstants.class.getSimpleName() + ".DEFAULT_LOCAL_DATE_TIME_PATTERN")
                .contains("private static " + LocalDateTime.class.getSimpleName() + " parseLocalDateTime_updatedDate(String value)")
                .contains("LocalDateTime.parse(value, FORMATTER_UPDATEDDATE)");
    }

    @Test
    @DisplayName("Should generate EnumFilter deserializer with @JsonCreator method")
    void shouldGenerateEnumFilterDeserializerWithJsonCreatorMethod() throws Exception {
        // Given
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo("com.test.UserStatus");
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.CREATOR_METHOD);
        enumInfo.setJsonCreatorMethod("fromValue");

        FilterFieldConfig enumConfig = new FilterFieldConfig(STATUS_FIELD, "com.test.UserStatus", "UserStatusFilter");
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(enumConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(ENUM_FILTER, "com.test", fieldConfigs, "StatusEntity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify package and class structure
        assertThat(generatedCode).contains("package com.test;")
                .contains("public class EnumFilterDeserializer extends " + JsonDeserializer.class.getSimpleName() + "<EnumFilter>")
                .contains("private static UserStatus parseUserStatus(String value)")
                .contains("// CASE: UserStatus has @JsonCreator method \"fromValue\"")
                .contains("return UserStatus.fromValue(value);")
                .contains("case \"status\"")
                .contains("parseUserStatus(");
    }

    @Test
    @DisplayName("Should generate EnumFilter deserializer with @JsonValue field")
    void shouldGenerateEnumFilterDeserializerWithJsonValueField() throws Exception {
        // Given
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo("com.test.Priority");
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.VALUE_FIELD);
        enumInfo.setJsonValueField("code");

        FilterFieldConfig enumConfig = new FilterFieldConfig("priority", "com.test.Priority", "PriorityFilter");
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(enumConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(ENUM_FILTER, "com.test", fieldConfigs, "TaskEntity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify enum parsing method generation
        assertThat(generatedCode).contains("private static Priority parsePriority(String value)")
                .contains("// CASE: Priority has @JsonValue on \"code\" field")
                .contains("for (Priority enumValue : Priority.values())")
                .contains("if (java.util.Objects.equals(enumValue.code, value))")
                .contains(RETURN_ENUM_VALUE)
                .contains("throw new IllegalArgumentException(\"Unknown Priority code: \" + value);");
    }

    @Test
    @DisplayName("Should generate EnumFilter deserializer with @JsonValue method")
    void shouldGenerateEnumFilterDeserializerWithJsonValueMethod() throws Exception {
        // Given
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo("com.test.Category");
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.VALUE_METHOD);
        enumInfo.setJsonValueMethod("getValue");

        FilterFieldConfig enumConfig = new FilterFieldConfig("category", "com.test.Category", "CategoryFilter");
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(enumConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(ENUM_FILTER, "com.test", fieldConfigs, "ProductEntity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify enum parsing method generation
        assertThat(generatedCode).contains("private static Category parseCategory(String value)")
                .contains("// CASE: Category has @JsonValue on \"getValue\" method")
                .contains("for (Category enumValue : Category.values())")
                .contains("if (java.util.Objects.equals(enumValue.getValue(), value))")
                .contains(RETURN_ENUM_VALUE)
                .contains("throw new IllegalArgumentException(\"Unknown Category value: \" + value);");
    }

    @Test
    @DisplayName("Should generate EnumFilter deserializer with default matching")
    void shouldGenerateEnumFilterDeserializerWithDefaultMatching() throws Exception {
        // Given
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo("com.test.State");
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.DEFAULT_MATCHING);

        FilterFieldConfig enumConfig = new FilterFieldConfig("state", "com.test.State", "StateFilter");
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(enumConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(ENUM_FILTER, "com.test", fieldConfigs, "OrderEntity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify enum parsing method generation
        assertThat(generatedCode).contains("private static State parseState(String value)")
                .contains("// CASE: No Jackson annotations - using valueOf() with case-insensitive fallback")
                .contains("try {").
                contains("return State.valueOf(value);")
                .contains("} catch (IllegalArgumentException e) {")
                .contains("for (State enumValue : State.values())")
                .contains("if (enumValue.name().equalsIgnoreCase(value))")
                .contains(RETURN_ENUM_VALUE)
                .contains("throw new IllegalArgumentException(\"Unknown State: \" + value);");
    }

    @Test
    @DisplayName("Should generate complete EnumFilter deserializer with all enum fields")
    void shouldGenerateCompleteEnumFilterDeserializer() throws Exception {
        // Given
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo("com.example.OrderStatus");
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.CREATOR_METHOD);
        enumInfo.setJsonCreatorMethod("fromCode");

        FilterFieldConfig enumConfig = new FilterFieldConfig("orderStatus", "com.example.OrderStatus", "OrderStatusFilter");
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(enumConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(ENUM_FILTER, COM_EXAMPLE_PACKAGE, fieldConfigs, "Order");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify enum parsing method is generated correctly
        assertThat(generatedCode).contains("private static OrderStatus parseOrderStatus(String value)")
                .contains("return OrderStatus.fromCode(value);")
                .contains("Performance: Direct object creation, no reflection")
                .contains("Performance: Streaming parser, no intermediate tree")
                .contains("Performance: Switch statement with interned strings")
                .contains("throw ctxt.wrongTokenException(p, EnumFilter.class, JsonToken.START_OBJECT, \"Expected start object\")")
                .contains("default -> p.skipChildren(); // Skip unknown fields efficiently");
    }

    @Test
    @DisplayName("Should generate complete LocalDateTimeFilter deserializer with all temporal fields")
    void shouldGenerateCompleteLocalDateTimeFilterDeserializer() throws Exception {
        // Given
        DateTimeFormatInfo formatInfo = DateTimeFormatInfo.withCustomPattern("yyyy-MM-dd'T'HH:mm:ss", LocalDateTime.class.getName());
        FilterFieldConfig temporalConfig = new FilterFieldConfig("timestamp",
                LocalDateTime.class.getName(), LOCAL_DATE_TIME_FILTER);
        temporalConfig.setTemporal(true);
        temporalConfig.setDateTimeFormatInfo(formatInfo);
        temporalConfig.setTemporal(true);
        temporalConfig.setDateTimeFormatInfo(formatInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(temporalConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(LOCAL_DATE_TIME_FILTER, COM_EXAMPLE_PACKAGE, fieldConfigs, "Event");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify performance optimizations are present
        assertThat(generatedCode).contains("Performance: Direct object creation, no reflection")
                .contains("Performance: Streaming parser, no intermediate tree")
                .contains("Performance: Switch statement with interned strings")
                .contains("throw ctxt.wrongTokenException(p, LocalDateTimeFilter.class, JsonToken.START_OBJECT, \"Expected start object\")")
                .contains("default -> p.skipChildren(); // Skip unknown fields efficiently")
                .contains("DateTimeFormatter.ofPattern(\"yyyy-MM-dd'T'HH:mm:ss\")")
                .contains("private static LocalDateTime parseLocalDateTime_timestamp(String value)");
    }

    @Test
    @DisplayName("Should generate StringFilter deserializer with all string operations")
    void shouldGenerateStringFilterDeserializerWithAllStringOperations() throws Exception {
        // Given
        FilterFieldConfig stringConfig = new FilterFieldConfig("name", "java.lang.String", "StringFilter");
        stringConfig.setString(true);
        List<FilterFieldConfig> fieldConfigs = List.of(stringConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("NameFilter", COM_EXAMPLE_PACKAGE, fieldConfigs, "Person");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify package and class structure
        assertThat(generatedCode).contains(PACKAGE_COM_EXAMPLE).contains("public class NameFilterDeserializer extends JsonDeserializer<NameFilter>")
                .contains("case \"name\"");
    }

    @Test
    @DisplayName("Should generate IntegerFilter deserializer with all numeric fields")
    void shouldGenerateIntegerFilterDeserializerWithAllNumericFields() throws Exception {
        // Given
        FilterFieldConfig integerConfig = new FilterFieldConfig("age", Integer.class.getName(), "IntegerFilter");
        integerConfig.setNumeric(true);
        List<FilterFieldConfig> fieldConfigs = List.of(integerConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("AgeFilter", COM_EXAMPLE_PACKAGE, fieldConfigs, "Person");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify package and class structure
        assertThat(generatedCode).contains(PACKAGE_COM_EXAMPLE).contains("public class AgeFilterDeserializer extends JsonDeserializer<AgeFilter>")
                .contains("case \"age\"");
    }

    @Test
    @DisplayName("Should generate LongFilter deserializer with correct parsing methods")
    void shouldGenerateLongFilterDeserializerWithCorrectParsingMethods() throws Exception {
        // Given
        FilterFieldConfig longConfig = new FilterFieldConfig("id", Long.class.getName(), "LongFilter");
        longConfig.setNumeric(true);
        List<FilterFieldConfig> fieldConfigs = List.of(longConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("IdFilter", COM_EXAMPLE_PACKAGE, fieldConfigs, "Entity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify package and class structure
        assertThat(generatedCode).contains(PACKAGE_COM_EXAMPLE)
                .contains("public class IdFilterDeserializer extends JsonDeserializer<IdFilter>")
                .contains("case \"id\"");
    }

    @Test
    @DisplayName("Should generate DoubleFilter deserializer with correct parsing methods")
    void shouldGenerateDoubleFilterDeserializerWithCorrectParsingMethods() throws Exception {
        // Given
        FilterFieldConfig doubleConfig = new FilterFieldConfig("price", Double.class.getName(), "DoubleFilter");
        doubleConfig.setNumeric(true);
        List<FilterFieldConfig> fieldConfigs = List.of(doubleConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("PriceFilter", COM_EXAMPLE_PACKAGE, fieldConfigs, "Product");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify package and class structure
        assertThat(generatedCode).contains(PACKAGE_COM_EXAMPLE)
                .contains("public class PriceFilterDeserializer extends JsonDeserializer<PriceFilter>")
                .contains("case \"price\"");
    }

    @Test
    @DisplayName("Should generate complex filter with multiple field types")
    void shouldGenerateComplexFilterWithMultipleFieldTypes() throws Exception {
        // Given
        DateTimeFormatInfo formatInfo = DateTimeFormatInfo.withDefaultPattern(LocalDateTime.class.getName());
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo(COM_TEST_STATUS);
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.DEFAULT_MATCHING);

        FilterFieldConfig nameConfig = new FilterFieldConfig("name", "java.lang.String", "StringFilter");
        nameConfig.setString(true);

        FilterFieldConfig ageConfig = new FilterFieldConfig("age", Integer.class.getName(), "IntegerFilter");
        ageConfig.setNumeric(true);

        FilterFieldConfig statusConfig = new FilterFieldConfig(STATUS_FIELD, COM_TEST_STATUS, "StatusFilter");
        statusConfig.setEnum(true);
        statusConfig.setEnumDeserializationInfo(enumInfo);

        FilterFieldConfig createdAtConfig = new FilterFieldConfig("createdAt", LocalDateTime.class.getName(), LOCAL_DATE_TIME_FILTER);
        createdAtConfig.setTemporal(true);
        createdAtConfig.setDateTimeFormatInfo(formatInfo);

        List<FilterFieldConfig> fieldConfigs = Arrays.asList(
                nameConfig,
                ageConfig,
                statusConfig,
                createdAtConfig
        );

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("ComplexFilter", "com.test", fieldConfigs, "ComplexEntity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify all field types are handled
        assertThat(generatedCode).contains("case \"name\"").contains("case \"age\"").contains("case \"status\"").contains("case \"createdAt\"")
                .contains("private static Status parseStatus(String value)")
                .contains("private static LocalDateTime parseLocalDateTime_createdAt(String value)");
    }

    /**
     * Property 1: Model type detection
     * Validates: Requirements 2.1
     * 
     * For any collection field with a model element type (where a corresponding filter class exists),
     * processCollectionField should correctly detect it as a model type and set isModelElementType to true.
     */
    @Property
    @DisplayName("Property 1: Model type detection - processCollectionField detects model types correctly")
    void processCollectionFieldDetectsModelTypes(@ForAll("modelTypeNames") String modelTypeName) {
        // Given: A collection field config with a model element type
        FilterFieldConfig config = new FilterFieldConfig(ITEMS_FIELD, JAVA_UTIL_COLLECTION, COLLECTION_FILTER);
        config.setCollection(true);
        config.setElementType(modelTypeName);
        config.setPackageName(TESTMODEL_PACKAGE);

        // Mock the processing environment to simulate filter class existence
        TypeElement mockFilterElement = org.mockito.Mockito.mock(TypeElement.class);
        String expectedFilterClassName = modelTypeName + "Filter";
        when(elements.getTypeElement(anyString())).thenAnswer(invocation -> {
            String className = invocation.getArgument(0);
            // Return mock element if it's the expected filter class
            if (className.endsWith(expectedFilterClassName)) {
                return mockFilterElement;
            }
            return null;
        });

        // When: processCollectionField is called
        generator.processCollectionField(config);

        // Then: The config should be marked as having a model element type
        assertThat(config.isModelElementType())
                .as("Collection with element type %s should be detected as model type", modelTypeName)
                .isTrue();
        
        assertThat(config.getElementFilterType())
                .as("Element filter type should be set correctly")
                .isEqualTo(expectedFilterClassName);
        
        assertThat(config.getElementFilterPackage())
                .as("Element filter package should be extracted from element type")
                .isNotEmpty();
    }

    /**
     * Property 1 (negative case): Basic type detection
     * Validates: Requirements 2.1
     * 
     * For any collection field with a basic element type (String, Integer, etc.),
     * processCollectionField should correctly detect it as NOT a model type.
     */
    @Property
    @DisplayName("Property 1 (negative): Basic type detection - processCollectionField detects basic types correctly")
    void processCollectionFieldDetectsBasicTypes(@ForAll("basicTypeNames") String basicTypeName) {
        // Given: A collection field config with a basic element type
        FilterFieldConfig config = new FilterFieldConfig(ITEMS_FIELD, JAVA_UTIL_COLLECTION, COLLECTION_FILTER);
        config.setCollection(true);
        config.setElementType(basicTypeName);
        config.setPackageName(TESTMODEL_PACKAGE);

        // Mock the processing environment to simulate NO filter class exists for basic types
        when(elements.getTypeElement(anyString())).thenReturn(null);

        // When: processCollectionField is called
        generator.processCollectionField(config);

        // Then: The config should NOT be marked as having a model element type
        assertThat(config.isModelElementType())
                .as("Collection with basic element type %s should NOT be detected as model type", basicTypeName)
                .isFalse();
        
        assertThat(config.getElementFilterType())
                .as("Element filter type should not be set for basic types")
                .isNull();
    }

    @Provide
    Arbitrary<String> modelTypeNames() {
        return Arbitraries.of(
                "com.thy.fss.common.inmemory.testmodel.User",
                "com.thy.fss.common.inmemory.testmodel.Address",
                "com.thy.fss.common.inmemory.testmodel.Order",
                "com.thy.fss.common.inmemory.testmodel.Profile",
                "com.thy.fss.common.inmemory.testmodel.Customer",
                "com.thy.fss.common.inmemory.testmodel.Product",
                "com.thy.fss.common.inmemory.testmodel.OrderItem"
        );
    }

    @Provide
    Arbitrary<String> basicTypeNames() {
        return Arbitraries.of(
                "java.lang.String",
                "java.lang.Integer",
                "java.lang.Long",
                "java.lang.Double",
                "java.lang.Boolean",
                "java.time.LocalDate",
                "java.time.LocalDateTime",
                "java.time.Instant"
        );
    }

    /**
     * Property 2: Element filter class resolution
     * Validates: Requirements 2.2
     * 
     * For any model type name, processCollectionField should correctly resolve
     * the element filter class name and package.
     */
    @Property
    @DisplayName("Property 2: Element filter class resolution - processCollectionField resolves filter class names correctly")
    void processCollectionFieldResolvesFilterClassNames(@ForAll("qualifiedModelTypeNames") QualifiedModelType modelType) {
        // Given: A collection field config with a fully qualified model element type
        FilterFieldConfig config = new FilterFieldConfig(ITEMS_FIELD, JAVA_UTIL_COLLECTION, COLLECTION_FILTER);
        config.setCollection(true);
        config.setElementType(modelType.fullyQualifiedName);
        config.setPackageName(TESTMODEL_PACKAGE);

        // Mock the processing environment to simulate filter class existence
        TypeElement mockFilterElement = org.mockito.Mockito.mock(TypeElement.class);
        when(elements.getTypeElement(anyString())).thenAnswer(invocation -> {
            String className = invocation.getArgument(0);
            // Return mock element if it's the expected filter class
            if (className.equals(modelType.expectedFilterQualifiedName)) {
                return mockFilterElement;
            }
            return null;
        });

        // When: processCollectionField is called
        generator.processCollectionField(config);

        // Then: The filter class name and package should be resolved correctly
        assertThat(config.getElementFilterType())
                .as("Element filter type should be resolved correctly for %s", modelType.fullyQualifiedName)
                .isEqualTo(modelType.expectedFilterSimpleName);
        
        assertThat(config.getElementFilterPackage())
                .as("Element filter package should be extracted correctly for %s", modelType.fullyQualifiedName)
                .isEqualTo(modelType.expectedPackage);
        
        assertThat(config.getElementFilterQualifiedName())
                .as("Element filter qualified name should be constructed correctly for %s", modelType.fullyQualifiedName)
                .isEqualTo(modelType.expectedFilterQualifiedName);
    }

    @Provide
    Arbitrary<QualifiedModelType> qualifiedModelTypeNames() {
        return Arbitraries.of(
                new QualifiedModelType(
                        "com.thy.fss.common.inmemory.testmodel.User",
                        TESTMODEL_PACKAGE,
                        "UserFilter",
                        "com.thy.fss.common.inmemory.testmodel.UserFilter"
                ),
                new QualifiedModelType(
                        "com.thy.fss.common.inmemory.testmodel.Address",
                        TESTMODEL_PACKAGE,
                        "AddressFilter",
                        "com.thy.fss.common.inmemory.testmodel.AddressFilter"
                ),
                new QualifiedModelType(
                        "com.thy.fss.common.inmemory.testmodel.Order",
                        TESTMODEL_PACKAGE,
                        "OrderFilter",
                        "com.thy.fss.common.inmemory.testmodel.OrderFilter"
                ),
                new QualifiedModelType(
                        "com.example.model.Product",
                        "com.example.model",
                        "ProductFilter",
                        "com.example.model.ProductFilter"
                ),
                new QualifiedModelType(
                        "com.example.domain.Customer",
                        "com.example.domain",
                        "CustomerFilter",
                        "com.example.domain.CustomerFilter"
                )
        );
    }

    /**
     * Helper class for property test data
     */
    private static class QualifiedModelType {
        final String fullyQualifiedName;
        final String expectedPackage;
        final String expectedFilterSimpleName;
        final String expectedFilterQualifiedName;

        QualifiedModelType(String fullyQualifiedName, String expectedPackage, 
                          String expectedFilterSimpleName, String expectedFilterQualifiedName) {
            this.fullyQualifiedName = fullyQualifiedName;
            this.expectedPackage = expectedPackage;
            this.expectedFilterSimpleName = expectedFilterSimpleName;
            this.expectedFilterQualifiedName = expectedFilterQualifiedName;
        }
    }

    @Test
    @DisplayName("Should NOT generate deepCopy method")
    void shouldNotGenerateDeepCopyMethod() throws Exception {
        // Given
        FilterFieldConfig stringConfig = new FilterFieldConfig("name", "java.lang.String", "StringFilter");
        stringConfig.setString(true);
        List<FilterFieldConfig> fieldConfigs = List.of(stringConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("TestFilter", "com.test", fieldConfigs, "TestEntity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify deepCopy method is NOT generated
        assertThat(generatedCode)
                .as("Generated code should NOT contain deepCopy method")
                .doesNotContain("public TestEntity deepCopy(TestEntity source)")
                .doesNotContain("deepCopy(");
    }

    @Test
    @DisplayName("Should NOT implement EntityCopier interface")
    void shouldNotImplementEntityCopierInterface() throws Exception {
        // Given
        FilterFieldConfig stringConfig = new FilterFieldConfig("name", "java.lang.String", "StringFilter");
        stringConfig.setString(true);
        List<FilterFieldConfig> fieldConfigs = List.of(stringConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("TestFilter", "com.test", fieldConfigs, "TestEntity");

        // Then
        String generatedCode = stringWriter.toString();

        // Verify EntityCopier interface is NOT implemented
        assertThat(generatedCode)
                .as("Generated class should NOT implement EntityCopier interface")
                .doesNotContain("implements com.thy.fss.common.inmemory.copier.EntityCopier")
                .doesNotContain("implements EntityCopier");
    }

    // ===== INPUT VALIDATION TESTS =====

    private static final String NULL_FILTER_CLASS = null;
    private static final String EMPTY_FILTER_CLASS = "";
    private static final String INVALID_STARTS_WITH_DIGIT = "123InvalidFilter";
    private static final String INVALID_PACKAGE = "123.invalid";
    private static final String VALID_PACKAGE = "com.test";
    private static final String TEST_FILTER = "TestFilter";
    private static final String TEST_ENTITY = "TestEntity";
    private static final String BOOL_FIELD = "active";
    private static final String BOOL_FILTER_TYPE = "BooleanFilter";
    private static final String LOCAL_DATE_FIELD = "startDate";
    private static final String LOCAL_DATE_FILTER_TYPE = "LocalDateFilter";
    private static final String INSTANT_FIELD = "publishedAt";
    private static final String INSTANT_FILTER_TYPE = "InstantFilter";
    private static final String CATEGORY_FIELD = "category";
    private static final String TAGS_FIELD = "tags";
    private static final String JAVA_LANG_STRING = "java.lang.String";
    private static final String JAVA_LANG_LONG = "java.lang.Long";
    private static final String JAVA_LANG_DOUBLE = "java.lang.Double";
    private static final String NAME_FIELD = "name";
    private static final String PERSON_ENTITY = "Person";

    @Test
    @DisplayName("Should throw exception when filter class name is null")
    void shouldThrowExceptionWhenFilterClassNameIsNull() {
        // Given
        List<FilterFieldConfig> configs = List.of();

        // When & Then
        assertThatThrownBy(() -> generator.generateDeserializer(NULL_FILTER_CLASS, VALID_PACKAGE, configs, TEST_ENTITY))
                .isInstanceOf(DeserializerGenerationException.class);
    }

    @Test
    @DisplayName("Should throw exception when filter class name is empty")
    void shouldThrowExceptionWhenFilterClassNameIsEmpty() {
        // Given
        List<FilterFieldConfig> configs = List.of();

        // When & Then
        assertThatThrownBy(() -> generator.generateDeserializer(EMPTY_FILTER_CLASS, VALID_PACKAGE, configs, TEST_ENTITY))
                .isInstanceOf(DeserializerGenerationException.class);
    }

    @Test
    @DisplayName("Should throw exception when package name is null")
    void shouldThrowExceptionWhenPackageNameIsNull() {
        // Given
        List<FilterFieldConfig> configs = List.of();

        // When & Then
        assertThatThrownBy(() -> generator.generateDeserializer(TEST_FILTER, null, configs, TEST_ENTITY))
                .isInstanceOf(DeserializerGenerationException.class);
    }

    @Test
    @DisplayName("Should throw exception when field configs list is null")
    void shouldThrowExceptionWhenFieldConfigsIsNull() {
        // When & Then
        assertThatThrownBy(() -> generator.generateDeserializer(TEST_FILTER, VALID_PACKAGE, null, TEST_ENTITY))
                .isInstanceOf(DeserializerGenerationException.class);
    }

    @Test
    @DisplayName("Should throw exception when filter class name starts with digit")
    void shouldThrowExceptionWhenFilterClassNameHasInvalidFormat() {
        // Given
        List<FilterFieldConfig> configs = List.of();

        // When & Then
        assertThatThrownBy(() -> generator.generateDeserializer(INVALID_STARTS_WITH_DIGIT, VALID_PACKAGE, configs, TEST_ENTITY))
                .isInstanceOf(DeserializerGenerationException.class);
    }

    @Test
    @DisplayName("Should throw exception when package name starts with digit")
    void shouldThrowExceptionWhenPackageNameHasInvalidFormat() {
        // Given
        List<FilterFieldConfig> configs = List.of();

        // When & Then
        assertThatThrownBy(() -> generator.generateDeserializer(TEST_FILTER, INVALID_PACKAGE, configs, TEST_ENTITY))
                .isInstanceOf(DeserializerGenerationException.class);
    }

    @Test
    @DisplayName("Should throw exception when a field config in list is null")
    void shouldThrowExceptionWhenFieldConfigInListIsNull() throws Exception {
        // Given
        List<FilterFieldConfig> configs = new ArrayList<>();
        configs.add(null);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When & Then
        assertThatThrownBy(() -> generator.generateDeserializer(TEST_FILTER, VALID_PACKAGE, configs, TEST_ENTITY))
                .isInstanceOf(DeserializerGenerationException.class);
    }

    @Test
    @DisplayName("Should throw exception when field name is null")
    void shouldThrowExceptionWhenFieldNameIsNull() throws Exception {
        // Given
        FilterFieldConfig config = new FilterFieldConfig(null, JAVA_LANG_STRING, "StringFilter");
        config.setString(true);
        List<FilterFieldConfig> configs = List.of(config);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When & Then
        assertThatThrownBy(() -> generator.generateDeserializer(TEST_FILTER, VALID_PACKAGE, configs, TEST_ENTITY))
                .isInstanceOf(DeserializerGenerationException.class);
    }

    @Test
    @DisplayName("Should throw exception when field type is null")
    void shouldThrowExceptionWhenFieldTypeIsNull() throws Exception {
        // Given
        FilterFieldConfig config = new FilterFieldConfig(NAME_FIELD, null, "StringFilter");
        config.setString(true);
        List<FilterFieldConfig> configs = List.of(config);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When & Then
        assertThatThrownBy(() -> generator.generateDeserializer(TEST_FILTER, VALID_PACKAGE, configs, TEST_ENTITY))
                .isInstanceOf(DeserializerGenerationException.class);
    }

    @Test
    @DisplayName("Should throw exception when filter type is null")
    void shouldThrowExceptionWhenFilterTypeIsNull() throws Exception {
        // Given
        FilterFieldConfig config = new FilterFieldConfig(NAME_FIELD, JAVA_LANG_STRING, null);
        config.setString(true);
        List<FilterFieldConfig> configs = List.of(config);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When & Then
        assertThatThrownBy(() -> generator.generateDeserializer(TEST_FILTER, VALID_PACKAGE, configs, TEST_ENTITY))
                .isInstanceOf(DeserializerGenerationException.class);
    }

    @Test
    @DisplayName("Should throw exception when field name has invalid Java identifier characters")
    void shouldThrowExceptionWhenFieldNameHasInvalidChars() throws Exception {
        // Given
        FilterFieldConfig config = new FilterFieldConfig("123field", JAVA_LANG_STRING, "StringFilter");
        config.setString(true);
        List<FilterFieldConfig> configs = List.of(config);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When & Then
        assertThatThrownBy(() -> generator.generateDeserializer(TEST_FILTER, VALID_PACKAGE, configs, TEST_ENTITY))
                .isInstanceOf(DeserializerGenerationException.class);
    }

    @Test
    @DisplayName("Should throw DeserializerGenerationException on IOException from createSourceFile")
    void shouldThrowDeserializerExceptionWhenCreateSourceFileFails() throws Exception {
        // Given
        FilterFieldConfig stringConfig = new FilterFieldConfig(NAME_FIELD, JAVA_LANG_STRING, "StringFilter");
        stringConfig.setString(true);
        List<FilterFieldConfig> configs = List.of(stringConfig);

        when(filer.createSourceFile(anyString())).thenThrow(new IOException("disk error"));

        // When & Then
        assertThatThrownBy(() -> generator.generateDeserializer(TEST_FILTER, VALID_PACKAGE, configs, TEST_ENTITY))
                .isInstanceOf(DeserializerGenerationException.class)
                .hasMessageContaining("file_creation");
    }

    @Test
    @DisplayName("Should throw DeserializerGenerationException on IOException from openWriter")
    void shouldThrowDeserializerExceptionWhenOpenWriterFails() throws Exception {
        // Given
        FilterFieldConfig stringConfig = new FilterFieldConfig(NAME_FIELD, JAVA_LANG_STRING, "StringFilter");
        stringConfig.setString(true);
        List<FilterFieldConfig> configs = List.of(stringConfig);

        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenThrow(new IOException("write error"));

        // When & Then
        assertThatThrownBy(() -> generator.generateDeserializer(TEST_FILTER, VALID_PACKAGE, configs, TEST_ENTITY))
                .isInstanceOf(DeserializerGenerationException.class);
    }

    // ===== ADDITIONAL FIELD TYPE TESTS =====

    @Test
    @DisplayName("Should generate deserializer with empty field configs without exception")
    void shouldGenerateDeserializerWithEmptyFieldConfigs() throws Exception {
        // Given
        List<FilterFieldConfig> fieldConfigs = List.of();

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(TEST_FILTER, VALID_PACKAGE, fieldConfigs, TEST_ENTITY);

        // Then
        String generatedCode = stringWriter.toString();
        assertThat(generatedCode).contains("package com.test;")
                .contains("public class TestFilterDeserializer");
    }

    @Test
    @DisplayName("Should generate deserializer with empty package name")
    void shouldGenerateDeserializerWithEmptyPackageName() throws Exception {
        // Given
        FilterFieldConfig stringConfig = new FilterFieldConfig(NAME_FIELD, JAVA_LANG_STRING, "StringFilter");
        stringConfig.setString(true);
        List<FilterFieldConfig> fieldConfigs = List.of(stringConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(TEST_FILTER, "", fieldConfigs, TEST_ENTITY);

        // Then
        String generatedCode = stringWriter.toString();
        assertThat(generatedCode).contains("public class TestFilterDeserializer")
                .doesNotContain("package ;");
    }

    @Test
    @DisplayName("Should generate BooleanFilter deserializer")
    void shouldGenerateBooleanFilterDeserializer() throws Exception {
        // Given
        FilterFieldConfig boolConfig = new FilterFieldConfig(BOOL_FIELD, "java.lang.Boolean", BOOL_FILTER_TYPE);
        List<FilterFieldConfig> fieldConfigs = List.of(boolConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("ActiveFilter", COM_EXAMPLE_PACKAGE, fieldConfigs, "ActiveEntity");

        // Then
        String generatedCode = stringWriter.toString();
        assertThat(generatedCode).contains(PACKAGE_COM_EXAMPLE)
                .contains("public class ActiveFilterDeserializer extends JsonDeserializer<ActiveFilter>");
    }

    @Test
    @DisplayName("Should generate LocalDateFilter deserializer")
    void shouldGenerateLocalDateFilterDeserializer() throws Exception {
        // Given
        DateTimeFormatInfo formatInfo = DateTimeFormatInfo.withDefaultPattern(LocalDate.class.getName());
        FilterFieldConfig dateConfig = new FilterFieldConfig(LOCAL_DATE_FIELD, LocalDate.class.getName(), LOCAL_DATE_FILTER_TYPE);
        dateConfig.setTemporal(true);
        dateConfig.setDateTimeFormatInfo(formatInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(dateConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("DateFilter", COM_EXAMPLE_PACKAGE, fieldConfigs, "DateEntity");

        // Then
        String generatedCode = stringWriter.toString();
        assertThat(generatedCode).contains(PACKAGE_COM_EXAMPLE)
                .contains("public class DateFilterDeserializer extends JsonDeserializer<DateFilter>")
                .contains("import " + LocalDate.class.getName() + ";")
                .contains("FORMATTER_STARTDATE")
                .contains(FilterConstants.class.getSimpleName() + ".DEFAULT_LOCAL_DATE_PATTERN");
    }

    @Test
    @DisplayName("Should generate InstantFilter deserializer")
    void shouldGenerateInstantFilterDeserializer() throws Exception {
        // Given
        FilterFieldConfig instantConfig = new FilterFieldConfig(INSTANT_FIELD, Instant.class.getName(), INSTANT_FILTER_TYPE);
        instantConfig.setTemporal(true);
        List<FilterFieldConfig> fieldConfigs = List.of(instantConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("InstantTestFilter", COM_EXAMPLE_PACKAGE, fieldConfigs, "PublishedEntity");

        // Then
        String generatedCode = stringWriter.toString();
        assertThat(generatedCode).contains(PACKAGE_COM_EXAMPLE)
                .contains("public class InstantTestFilterDeserializer extends JsonDeserializer<InstantTestFilter>")
                .contains("import " + Instant.class.getName() + ";")
                .contains("FORMATTER_PUBLISHEDAT")
                .contains("Instant.from(");
    }

    @Test
    @DisplayName("Should generate CollectionFilter deserializer with String elements")
    void shouldGenerateCollectionFilterWithStringElements() throws Exception {
        // Given
        FilterFieldConfig collectionConfig = new FilterFieldConfig(TAGS_FIELD, "java.util.Collection", "CollectionFilter");
        collectionConfig.setCollection(true);
        collectionConfig.setElementType(JAVA_LANG_STRING);
        collectionConfig.setModelElementType(false);
        List<FilterFieldConfig> fieldConfigs = List.of(collectionConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("TagFilter", COM_EXAMPLE_PACKAGE, fieldConfigs, "TagEntity");

        // Then
        String generatedCode = stringWriter.toString();
        assertThat(generatedCode).contains(PACKAGE_COM_EXAMPLE)
                .contains("public class TagFilterDeserializer extends JsonDeserializer<TagFilter>");
    }

    @Test
    @DisplayName("Should generate deserializer with model filter field")
    void shouldGenerateDeserializerWithModelFilterField() throws Exception {
        // Given
        FilterFieldConfig modelConfig = new FilterFieldConfig("address", "com.example.Address", "AddressFilter");
        modelConfig.setModel(true);
        modelConfig.setPackageName(COM_EXAMPLE_PACKAGE);
        List<FilterFieldConfig> fieldConfigs = List.of(modelConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("PersonFilter", COM_EXAMPLE_PACKAGE, fieldConfigs, PERSON_ENTITY);

        // Then
        String generatedCode = stringWriter.toString();
        assertThat(generatedCode).contains(PACKAGE_COM_EXAMPLE)
                .contains("public class PersonFilterDeserializer extends JsonDeserializer<PersonFilter>");
    }

    @Test
    @DisplayName("Should generate deserializer with field having JsonProperty annotation")
    void shouldGenerateDeserializerWithJsonPropertyAnnotation() throws Exception {
        // Given
        FilterFieldConfig nameConfig = new FilterFieldConfig(NAME_FIELD, JAVA_LANG_STRING, "StringFilter");
        nameConfig.setString(true);
        AnnotationInfo jsonProperty = new AnnotationInfo("JsonProperty", Map.of("value", "\"n\""));
        nameConfig.setJacksonAnnotations(List.of(jsonProperty));
        List<FilterFieldConfig> fieldConfigs = List.of(nameConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(TEST_FILTER, VALID_PACKAGE, fieldConfigs, TEST_ENTITY);

        // Then
        String generatedCode = stringWriter.toString();
        assertThat(generatedCode).contains("package com.test;")
                .contains("public class TestFilterDeserializer");
    }

    @Test
    @DisplayName("Should generate InstantFilter deserializer with custom pattern")
    void shouldGenerateInstantFilterWithCustomFormat() throws Exception {
        // Given
        DateTimeFormatInfo formatInfo = DateTimeFormatInfo.withCustomPattern("yyyy-MM-dd'T'HH:mm:ssZ", Instant.class.getName());
        FilterFieldConfig instantConfig = new FilterFieldConfig(INSTANT_FIELD, Instant.class.getName(), INSTANT_FILTER_TYPE);
        instantConfig.setTemporal(true);
        instantConfig.setDateTimeFormatInfo(formatInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(instantConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("InstantCustomFilter", COM_EXAMPLE_PACKAGE, fieldConfigs, "EventEntity");

        // Then
        String generatedCode = stringWriter.toString();
        assertThat(generatedCode).contains(PACKAGE_COM_EXAMPLE)
                .contains("FORMATTER_PUBLISHEDAT")
                .contains("yyyy-MM-dd'T'HH:mm:ssZ");
    }

    // ===== getAdditionalImports DIRECT TESTS =====

    @Test
    @DisplayName("Should return LocalDate imports for temporal LocalDate field")
    void shouldReturnLocalDateImportsForTemporalLocalDateField() {
        // Given
        FilterFieldConfig localDateConfig = new FilterFieldConfig(LOCAL_DATE_FIELD, LocalDate.class.getName(), LOCAL_DATE_FILTER_TYPE);
        localDateConfig.setTemporal(true);
        List<FilterFieldConfig> configs = List.of(localDateConfig);

        // When
        Set<String> imports = generator.getAdditionalImports(configs);

        // Then
        assertThat(imports).contains(LocalDate.class.getName())
                .contains("com.thy.fss.common.inmemory.filter.LocalDateFilter");
    }

    @Test
    @DisplayName("Should return Instant imports for temporal Instant field")
    void shouldReturnInstantImportsForTemporalInstantField() {
        // Given
        FilterFieldConfig instantConfig = new FilterFieldConfig(INSTANT_FIELD, Instant.class.getName(), INSTANT_FILTER_TYPE);
        instantConfig.setTemporal(true);
        List<FilterFieldConfig> configs = List.of(instantConfig);

        // When
        Set<String> imports = generator.getAdditionalImports(configs);

        // Then
        assertThat(imports).contains(Instant.class.getName())
                .contains("com.thy.fss.common.inmemory.filter.InstantFilter");
    }

    @Test
    @DisplayName("Should return BooleanFilter import for boolean filter field")
    void shouldReturnBooleanFilterImport() {
        // Given
        FilterFieldConfig boolConfig = new FilterFieldConfig(BOOL_FIELD, "java.lang.Boolean", BOOL_FILTER_TYPE);
        List<FilterFieldConfig> configs = List.of(boolConfig);

        // When
        Set<String> imports = generator.getAdditionalImports(configs);

        // Then
        assertThat(imports).contains("com.thy.fss.common.inmemory.filter.BooleanFilter");
    }

    @Test
    @DisplayName("Should return model filter imports for model field with package")
    void shouldReturnModelFilterImportsForModelField() {
        // Given
        FilterFieldConfig modelConfig = new FilterFieldConfig("address", "com.example.Address", "AddressFilter");
        modelConfig.setModel(true);
        modelConfig.setPackageName(COM_EXAMPLE_PACKAGE);
        List<FilterFieldConfig> configs = List.of(modelConfig);

        // When
        Set<String> imports = generator.getAdditionalImports(configs);

        // Then
        assertThat(imports).contains("com.example.AddressFilter")
                .contains("com.example.AddressFilterDeserializer");
    }

    @Test
    @DisplayName("Should return LongFilter import for Long numeric field")
    void shouldReturnLongFilterImport() {
        // Given
        FilterFieldConfig longConfig = new FilterFieldConfig("count", JAVA_LANG_LONG, "LongFilter");
        longConfig.setNumeric(true);
        List<FilterFieldConfig> configs = List.of(longConfig);

        // When
        Set<String> imports = generator.getAdditionalImports(configs);

        // Then
        assertThat(imports).contains("com.thy.fss.common.inmemory.filter.LongFilter");
    }

    @Test
    @DisplayName("Should return DoubleFilter import for Double numeric field")
    void shouldReturnDoubleFilterImport() {
        // Given
        FilterFieldConfig doubleConfig = new FilterFieldConfig("amount", JAVA_LANG_DOUBLE, "DoubleFilter");
        doubleConfig.setNumeric(true);
        List<FilterFieldConfig> configs = List.of(doubleConfig);

        // When
        Set<String> imports = generator.getAdditionalImports(configs);

        // Then
        assertThat(imports).contains("com.thy.fss.common.inmemory.filter.DoubleFilter");
    }

    @Test
    @DisplayName("Should return CollectionFilter import for collection field")
    void shouldReturnCollectionFilterImportForCollectionField() {
        // Given
        FilterFieldConfig collectionConfig = new FilterFieldConfig(TAGS_FIELD, "java.util.Collection", "CollectionFilter");
        collectionConfig.setCollection(true);
        collectionConfig.setElementType(JAVA_LANG_STRING);
        collectionConfig.setModelElementType(false);
        when(elements.getTypeElement(JAVA_LANG_STRING)).thenReturn(null);
        List<FilterFieldConfig> configs = List.of(collectionConfig);

        // When
        Set<String> imports = generator.getAdditionalImports(configs);

        // Then
        assertThat(imports).contains("com.thy.fss.common.inmemory.filter.CollectionFilter")
                .contains("com.thy.fss.common.inmemory.filter.StringFilter");
    }

    @Test
    @DisplayName("Should return model element filter imports for collection with model element type")
    void shouldReturnModelElementImportsForCollectionWithModelElement() {
        // Given
        FilterFieldConfig collectionConfig = new FilterFieldConfig("users", "java.util.Collection", "CollectionFilter");
        collectionConfig.setCollection(true);
        collectionConfig.setElementType("com.example.User");
        collectionConfig.setModelElementType(true);
        collectionConfig.setElementFilterType("UserFilter");
        collectionConfig.setElementFilterPackage(COM_EXAMPLE_PACKAGE);
        List<FilterFieldConfig> configs = List.of(collectionConfig);

        // When
        Set<String> imports = generator.getAdditionalImports(configs);

        // Then
        assertThat(imports).contains("com.thy.fss.common.inmemory.filter.CollectionFilter")
                .contains("com.example.UserFilter");
    }

    @Test
    @DisplayName("Should return EnumFilter import for collection with enum element type")
    void shouldReturnEnumFilterImportForCollectionWithEnumElement() {
        // Given
        FilterFieldConfig collectionConfig = new FilterFieldConfig("statuses", "java.util.Collection", "CollectionFilter");
        collectionConfig.setCollection(true);
        collectionConfig.setElementType("com.test.Status");
        collectionConfig.setModelElementType(false);

        TypeElement enumElement = mock(TypeElement.class);
        when(enumElement.getKind()).thenReturn(ElementKind.ENUM);
        when(elements.getTypeElement("com.test.Status")).thenReturn(enumElement);
        List<FilterFieldConfig> configs = List.of(collectionConfig);

        // When
        Set<String> imports = generator.getAdditionalImports(configs);

        // Then
        assertThat(imports).contains("com.thy.fss.common.inmemory.filter.CollectionFilter")
                .contains("com.thy.fss.common.inmemory.filter.EnumFilter")
                .contains("com.test.Status");
    }

    @Test
    @DisplayName("Should return empty set for empty field configs list")
    void shouldReturnEmptyImportsForEmptyFieldConfigs() {
        // When
        Set<String> imports = generator.getAdditionalImports(List.of());

        // Then
        assertThat(imports).isEmpty();
    }

    // ===== generateTemporalFieldConstant DIRECT TESTS =====

    @Test
    @DisplayName("Should generate LocalDate temporal field constant with default pattern")
    void shouldGenerateLocalDateTemporalFieldConstant() {
        // Given
        FilterFieldConfig localDateConfig = new FilterFieldConfig(LOCAL_DATE_FIELD, LocalDate.class.getName(), LOCAL_DATE_FILTER_TYPE);
        localDateConfig.setTemporal(true);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // When
        generator.generateTemporalFieldConstant(pw, localDateConfig);
        pw.flush();

        // Then
        assertThat(sw.toString())
                .contains("FORMATTER_STARTDATE")
                .contains(FilterConstants.class.getSimpleName() + ".DEFAULT_LOCAL_DATE_PATTERN");
    }

    @Test
    @DisplayName("Should generate Instant temporal field constant with default pattern")
    void shouldGenerateInstantTemporalFieldConstant() {
        // Given
        FilterFieldConfig instantConfig = new FilterFieldConfig(INSTANT_FIELD, Instant.class.getName(), INSTANT_FILTER_TYPE);
        instantConfig.setTemporal(true);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // When
        generator.generateTemporalFieldConstant(pw, instantConfig);
        pw.flush();

        // Then
        assertThat(sw.toString())
                .contains("FORMATTER_PUBLISHEDAT")
                .contains(FilterConstants.class.getSimpleName() + ".DEFAULT_INSTANT_PATTERN");
    }

    @Test
    @DisplayName("Should generate LocalDateTime temporal field constant with custom pattern")
    void shouldGenerateLocalDateTimeTemporalFieldConstantWithCustomPattern() {
        // Given
        DateTimeFormatInfo formatInfo = DateTimeFormatInfo.withCustomPattern("yyyy/MM/dd HH:mm", LocalDateTime.class.getName());
        FilterFieldConfig dtConfig = new FilterFieldConfig(CREATED_DATE_FIELD, LocalDateTime.class.getName(), LOCAL_DATE_TIME_FILTER);
        dtConfig.setTemporal(true);
        dtConfig.setDateTimeFormatInfo(formatInfo);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // When
        generator.generateTemporalFieldConstant(pw, dtConfig);
        pw.flush();

        // Then
        assertThat(sw.toString())
                .contains("FORMATTER_CREATEDDATE")
                .contains("yyyy/MM/dd HH:mm");
    }

    // ===== generateArrayParsingTemplate, generateSingleValueParsingTemplate, generateBooleanParsingTemplate TESTS =====

    @Test
    @DisplayName("Should generate array parsing template code")
    void shouldGenerateArrayParsingTemplateCode() {
        // Given
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // When
        generator.generateArrayParsingTemplate(pw, "String", "p.getText()", "setIn");
        pw.flush();

        // Then
        assertThat(sw.toString())
                .contains("START_ARRAY")
                .contains("List<String>")
                .contains("setIn")
                .contains("Pre-sized ArrayList");
    }

    @Test
    @DisplayName("Should generate single value parsing template code")
    void shouldGenerateSingleValueParsingTemplateCode() {
        // Given
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // When
        generator.generateSingleValueParsingTemplate(pw, "p.getText()", "setEquals");
        pw.flush();

        // Then
        assertThat(sw.toString())
                .contains("VALUE_STRING")
                .contains("setEquals")
                .contains("Direct parsing");
    }

    @Test
    @DisplayName("Should generate boolean parsing template code")
    void shouldGenerateBooleanParsingTemplateCode() {
        // Given
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // When
        generator.generateBooleanParsingTemplate(pw, "setIsNull");
        pw.flush();

        // Then
        assertThat(sw.toString())
                .contains("VALUE_TRUE")
                .contains("VALUE_FALSE")
                .contains("setIsNull");
    }

    // ===== processCollectionField EDGE CASE TESTS =====

    @Test
    @DisplayName("Should skip null config in processCollectionField without exception")
    void shouldSkipNullConfigInProcessCollectionField() {
        // When & Then - should not throw
        generator.processCollectionField(null);
    }

    @Test
    @DisplayName("Should skip non-collection config in processCollectionField")
    void shouldSkipNonCollectionConfigInProcessCollectionField() {
        // Given
        FilterFieldConfig config = new FilterFieldConfig(NAME_FIELD, JAVA_LANG_STRING, "StringFilter");
        config.setString(true);

        // When
        generator.processCollectionField(config);

        // Then - modelElementType should not be set
        assertThat(config.isModelElementType()).isFalse();
    }

    @Test
    @DisplayName("Should skip collection config with null element type")
    void shouldSkipCollectionConfigWithNullElementType() {
        // Given
        FilterFieldConfig config = new FilterFieldConfig(TAGS_FIELD, "java.util.Collection", "CollectionFilter");
        config.setCollection(true);
        config.setElementType(null);

        // When
        generator.processCollectionField(config);

        // Then
        assertThat(config.isModelElementType()).isFalse();
    }

    @Test
    @DisplayName("Should skip collection config with empty element type")
    void shouldSkipCollectionConfigWithEmptyElementType() {
        // Given
        FilterFieldConfig config = new FilterFieldConfig(TAGS_FIELD, "java.util.Collection", "CollectionFilter");
        config.setCollection(true);
        config.setElementType("");

        // When
        generator.processCollectionField(config);

        // Then
        assertThat(config.isModelElementType()).isFalse();
    }

    @Test
    @DisplayName("Should mark collection config as basic type when no filter class exists")
    void shouldMarkCollectionConfigAsBasicTypeWhenNoFilterClassExists() {
        // Given
        FilterFieldConfig config = new FilterFieldConfig(TAGS_FIELD, "java.util.Collection", "CollectionFilter");
        config.setCollection(true);
        config.setElementType(JAVA_LANG_STRING);
        config.setPackageName(TESTMODEL_PACKAGE);

        when(elements.getTypeElement(anyString())).thenReturn(null);

        // When
        generator.processCollectionField(config);

        // Then
        assertThat(config.isModelElementType()).isFalse();
    }

    // ===== debugLog TESTS =====

    @Test
    @DisplayName("Should invoke debugLog without throwing exception")
    void shouldInvokeDebugLogWithoutException() {
        // When & Then - should not throw even if debug mode is off
        generator.debugLog("test message");
    }

    // ===== generateSwitchCases DIRECT TESTS =====

    @Test
    @DisplayName("Should generate switch cases for temporal field")
    void shouldGenerateSwitchCasesForTemporalField() {
        // Given
        DateTimeFormatInfo formatInfo = DateTimeFormatInfo.withDefaultPattern(LocalDateTime.class.getName());
        FilterFieldConfig temporalConfig = new FilterFieldConfig(CREATED_DATE_FIELD, LocalDateTime.class.getName(), LOCAL_DATE_TIME_FILTER);
        temporalConfig.setTemporal(true);
        temporalConfig.setDateTimeFormatInfo(formatInfo);
        List<FilterFieldConfig> configs = List.of(temporalConfig);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // When
        generator.generateSwitchCases(pw, configs);
        pw.flush();

        // Then
        assertThat(sw.toString())
                .contains("FIELD_BEFORE")
                .contains("FIELD_AFTER");
    }

    @Test
    @DisplayName("Should generate switch cases for string field")
    void shouldGenerateSwitchCasesForStringField() {
        // Given
        FilterFieldConfig stringConfig = new FilterFieldConfig(NAME_FIELD, JAVA_LANG_STRING, "StringFilter");
        stringConfig.setString(true);
        List<FilterFieldConfig> configs = List.of(stringConfig);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // When
        generator.generateSwitchCases(pw, configs);
        pw.flush();

        // Then
        assertThat(sw.toString())
                .contains("FIELD_CONT")
                .contains("FIELD_START");
    }

    @Test
    @DisplayName("Should generate switch cases for numeric field")
    void shouldGenerateSwitchCasesForNumericField() {
        // Given
        FilterFieldConfig integerConfig = new FilterFieldConfig("age", Integer.class.getName(), "IntegerFilter");
        integerConfig.setNumeric(true);
        List<FilterFieldConfig> configs = List.of(integerConfig);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // When
        generator.generateSwitchCases(pw, configs);
        pw.flush();

        // Then
        assertThat(sw.toString())
                .contains("FIELD_GT")
                .contains("FIELD_LT");
    }

    @Test
    @DisplayName("Should generate switch cases for enum field")
    void shouldGenerateSwitchCasesForEnumField() {
        // Given
        EnumDeserializationInfo enumInfo = new EnumDeserializationInfo(COM_TEST_STATUS);
        enumInfo.setDeserializationType(EnumDeserializationInfo.DeserializationType.DEFAULT_MATCHING);
        FilterFieldConfig enumConfig = new FilterFieldConfig(STATUS_FIELD, COM_TEST_STATUS, ENUM_FILTER);
        enumConfig.setEnum(true);
        enumConfig.setEnumDeserializationInfo(enumInfo);
        List<FilterFieldConfig> configs = List.of(enumConfig);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // When
        generator.generateSwitchCases(pw, configs);
        pw.flush();

        // Then
        assertThat(sw.toString())
                .contains("FIELD_EQ")
                .contains("FIELD_IN");
    }

    @Test
    @DisplayName("Should generate switch cases for collection field")
    void shouldGenerateSwitchCasesForCollectionField() {
        // Given
        FilterFieldConfig collectionConfig = new FilterFieldConfig(TAGS_FIELD, "java.util.Collection", "CollectionFilter");
        collectionConfig.setCollection(true);
        collectionConfig.setElementType(JAVA_LANG_STRING);
        List<FilterFieldConfig> configs = List.of(collectionConfig);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // When
        generator.generateSwitchCases(pw, configs);
        pw.flush();

        // Then
        assertThat(sw.toString())
                .contains("FIELD_CONT")
                .contains("FIELD_EMPTY");
    }

    @Test
    @DisplayName("Should not generate switch cases for empty field configs")
    void shouldNotGenerateSwitchCasesForEmptyFieldConfigs() {
        // Given
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // When
        generator.generateSwitchCases(pw, List.of());
        pw.flush();

        // Then
        assertThat(sw.toString()).isEmpty();
    }

    @Test
    @DisplayName("Should generate complete LocalDateFilter deserializer with LocalDate type")
    void shouldGenerateCompleteLocalDateFilterDeserializeMethod() throws Exception {
        // Given
        DateTimeFormatInfo formatInfo = DateTimeFormatInfo.withCustomPattern("yyyy-MM-dd", LocalDate.class.getName());
        FilterFieldConfig dateConfig = new FilterFieldConfig("eventDate", LocalDate.class.getName(), LOCAL_DATE_FILTER_TYPE);
        dateConfig.setTemporal(true);
        dateConfig.setDateTimeFormatInfo(formatInfo);
        List<FilterFieldConfig> fieldConfigs = List.of(dateConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer(LOCAL_DATE_FILTER_TYPE, COM_EXAMPLE_PACKAGE, fieldConfigs, "EventEntity");

        // Then
        String generatedCode = stringWriter.toString();
        assertThat(generatedCode)
                .contains("private static final " + DateTimeFormatter.class.getSimpleName() + " FORMATTER_EVENTDATE")
                .contains("DateTimeFormatter.ofPattern(\"yyyy-MM-dd\")")
                .contains("private static LocalDate parseLocalDate_eventDate(String value)")
                .contains("LocalDate.parse(value, FORMATTER_EVENTDATE)");
    }

    @Test
    @DisplayName("Should generate CollectionFilter deserializer with Long element type")
    void shouldGenerateCollectionFilterWithLongElements() throws Exception {
        // Given
        FilterFieldConfig collectionConfig = new FilterFieldConfig("counts", "java.util.Collection", "CollectionFilter");
        collectionConfig.setCollection(true);
        collectionConfig.setElementType(JAVA_LANG_LONG);
        collectionConfig.setModelElementType(false);
        when(elements.getTypeElement(JAVA_LANG_LONG)).thenReturn(null);
        List<FilterFieldConfig> fieldConfigs = List.of(collectionConfig);

        StringWriter stringWriter = new StringWriter();
        when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // When
        generator.generateDeserializer("CountFilter", COM_EXAMPLE_PACKAGE, fieldConfigs, "CountEntity");

        // Then
        String generatedCode = stringWriter.toString();
        assertThat(generatedCode)
                .contains("public class CountFilterDeserializer extends JsonDeserializer<CountFilter>");
    }

    @Test
    @DisplayName("Should return Long element filter imports for collection with Long element")
    void shouldReturnLongElementFilterImportForCollection() {
        // Given
        FilterFieldConfig collectionConfig = new FilterFieldConfig("counts", "java.util.Collection", "CollectionFilter");
        collectionConfig.setCollection(true);
        collectionConfig.setElementType(JAVA_LANG_LONG);
        collectionConfig.setModelElementType(false);
        when(elements.getTypeElement(JAVA_LANG_LONG)).thenReturn(null);
        List<FilterFieldConfig> configs = List.of(collectionConfig);

        // When
        Set<String> imports = generator.getAdditionalImports(configs);

        // Then
        assertThat(imports).contains("com.thy.fss.common.inmemory.filter.CollectionFilter")
                .contains("com.thy.fss.common.inmemory.filter.LongFilter");
    }

    @Test
    @DisplayName("Should return Double element filter imports for collection with Double element")
    void shouldReturnDoubleElementFilterImportForCollection() {
        // Given
        FilterFieldConfig collectionConfig = new FilterFieldConfig("prices", "java.util.Collection", "CollectionFilter");
        collectionConfig.setCollection(true);
        collectionConfig.setElementType(JAVA_LANG_DOUBLE);
        collectionConfig.setModelElementType(false);
        when(elements.getTypeElement(JAVA_LANG_DOUBLE)).thenReturn(null);
        List<FilterFieldConfig> configs = List.of(collectionConfig);

        // When
        Set<String> imports = generator.getAdditionalImports(configs);

        // Then
        assertThat(imports).contains("com.thy.fss.common.inmemory.filter.CollectionFilter")
                .contains("com.thy.fss.common.inmemory.filter.DoubleFilter");
    }
}
