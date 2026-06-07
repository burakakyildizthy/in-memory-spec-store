package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.processor.MetaModel;
import com.thy.fss.common.inmemory.processor.exception.ProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("FilterMetaModelGenerator Tests")
class FilterMetaModelGeneratorTest {

    @Mock
    private ProcessingEnvironment processingEnv;

    @Mock
    private Messager messager;

    @Mock
    private Filer filer;

    @Mock
    private JavaFileObject javaFileObject;

    private StringWriter stringWriter;
    private FilterMetaModelGenerator generator;

    @BeforeEach
    void setUp() throws IOException {
        stringWriter = new StringWriter();

        lenient().when(processingEnv.getMessager()).thenReturn(messager);
        lenient().when(processingEnv.getFiler()).thenReturn(filer);
        lenient().when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        lenient().when(javaFileObject.openWriter()).thenReturn(stringWriter);

        generator = new FilterMetaModelGenerator(processingEnv);
    }

    @Test
    @DisplayName("Should generate basic filter class")
    void shouldGenerateBasicFilterClass() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement("com.test.User");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("package com.test;")),
                () -> assertTrue(generatedCode.contains("public class UserFilter implements EntityFilter<User>")),
                () -> assertTrue(generatedCode.contains("private static final long serialVersionUID = 1L;")),
                () -> assertTrue(generatedCode.contains("public UserFilter()")),
                () -> assertTrue(generatedCode.contains("public UserFilter(UserFilter filter)"))
        );
    }

    @Test
    @DisplayName("Should generate filter for class in default package")
    void shouldGenerateFilterForDefaultPackage() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement("User");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertFalse(generatedCode.contains("package")),
                () -> assertTrue(generatedCode.contains("public class UserFilter implements EntityFilter<User>"))
        );
    }

    @Test
    @DisplayName("Should generate string field filter")
    void shouldGenerateStringFieldFilter() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStringField("com.test.User", "name");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private StringFilter name;")),
                () -> assertTrue(generatedCode.contains("public StringFilter getName()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setName(StringFilter name)")),
                () -> assertTrue(generatedCode.contains("import com.thy.fss.common.inmemory.filter.StringFilter;"))
        );
    }

    @Test
    @DisplayName("Should generate integer field filter")
    void shouldGenerateIntegerFieldFilter() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithIntegerField("com.test.User", "age");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private IntegerFilter age;")),
                () -> assertTrue(generatedCode.contains("public IntegerFilter getAge()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setAge(IntegerFilter age)")),
                () -> assertTrue(generatedCode.contains("import com.thy.fss.common.inmemory.filter.IntegerFilter;"))
        );
    }

    @Test
    @DisplayName("Should generate boolean field filter")
    void shouldGenerateBooleanFieldFilter() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithBooleanField("com.test.User", "active");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private BooleanFilter active;")),
                () -> assertTrue(generatedCode.contains("public BooleanFilter getActive()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setActive(BooleanFilter active)"))
        );
    }

    @Test
    @DisplayName("Should generate LocalDate field filter")
    void shouldGenerateLocalDateFieldFilter() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithLocalDateField("com.test.User", "birthDate");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private LocalDateFilter birthDate;")),
                () -> assertTrue(generatedCode.contains("public LocalDateFilter getBirthDate()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setBirthDate(LocalDateFilter birthDate)"))
        );
    }

    @Test
    @DisplayName("Should generate builder pattern")
    void shouldGenerateBuilderPattern() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStringField("com.test.User", "name");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public static UserFilterBuilder builder()")),
                () -> assertTrue(generatedCode.contains("public static class UserFilterBuilder")),
                () -> assertTrue(generatedCode.contains("public UserFilter build()")),
                () -> assertTrue(generatedCode.contains("public UserFilter(UserFilter filter)")),
                () -> assertTrue(generatedCode.contains("this.name = filter.name != null ?"))
        );
    }

    @Test
    @DisplayName("Should skip static fields")
    void shouldSkipStaticFields() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStaticField("com.test.User", "CONSTANT");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertFalse(generatedCode.contains("CONSTANT"));
    }

    @Test
    @DisplayName("Should handle multiple field types")
    void shouldHandleMultipleFieldTypes() throws Exception {
        // Given
        TypeElement typeElement = createComplexTypeElement();

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private StringFilter name;")),
                () -> assertTrue(generatedCode.contains("private IntegerFilter age;")),
                () -> assertTrue(generatedCode.contains("private BooleanFilter active;"))
        );
    }

    @Test
    @DisplayName("Should throw ProcessingException on IOException")
    void shouldThrowProcessingExceptionOnIOException() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement("com.test.User");
        when(javaFileObject.openWriter()).thenThrow(new IOException("Test IO error"));

        // When & Then
        ProcessingException exception = assertThrows(ProcessingException.class,
                () -> generator.generate(typeElement));

        assertTrue(exception.getMessage().contains("Failed to generate FilterMetaModel"));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    @DisplayName("Should throw ProcessingException on unexpected error")
    void shouldThrowProcessingExceptionOnUnexpectedError() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement("com.test.User");
        when(javaFileObject.openWriter()).thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        ProcessingException exception = assertThrows(ProcessingException.class,
                () -> generator.generate(typeElement));

        assertTrue(exception.getMessage().contains("Unexpected error during FilterMetaModel generation"));
        assertInstanceOf(RuntimeException.class, exception.getCause());
    }

    @Test
    @DisplayName("Should generate enum field filter")
    void shouldGenerateEnumFieldFilter() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithEnumField("com.test.User", "status");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private EnumFilter<com.test.Status> status;")),
                () -> assertTrue(generatedCode.contains("public EnumFilter<com.test.Status> getStatus()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setStatus(EnumFilter<com.test.Status> status)")),
                () -> assertTrue(generatedCode.contains("import com.thy.fss.common.inmemory.filter.EnumFilter;"))
        );
    }

    @Test
    @DisplayName("Should generate collection field filter")
    void shouldGenerateCollectionFieldFilter() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithCollectionField("com.test.User", "tags");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private CollectionFilter<java.lang.String> tags;")),
                () -> assertTrue(generatedCode.contains("public CollectionFilter<java.lang.String> getTags()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setTags(CollectionFilter<java.lang.String> tags)")),
                () -> assertTrue(generatedCode.contains("import com.thy.fss.common.inmemory.filter.CollectionFilter;"))
        );
    }

    @Test
    @DisplayName("Should generate nested model field filter")
    void shouldGenerateNestedModelFieldFilter() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithNestedModelField("com.test.User", "profile");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private ProfileFilter profile;")),
                () -> assertTrue(generatedCode.contains("public ProfileFilter getProfile()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setProfile(ProfileFilter profile)"))
        );
    }

    @Test
    @DisplayName("Should generate double field filter")
    void shouldGenerateDoubleFieldFilter() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithDoubleField("com.test.User", "salary");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private DoubleFilter salary;")),
                () -> assertTrue(generatedCode.contains("public DoubleFilter getSalary()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setSalary(DoubleFilter salary)")),
                () -> assertTrue(generatedCode.contains("import com.thy.fss.common.inmemory.filter.DoubleFilter;"))
        );
    }

    @Test
    @DisplayName("Should generate long field filter")
    void shouldGenerateLongFieldFilter() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithLongField("com.test.User", "id");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private LongFilter id;")),
                () -> assertTrue(generatedCode.contains("public LongFilter getId()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setId(LongFilter id)")),
                () -> assertTrue(generatedCode.contains("import com.thy.fss.common.inmemory.filter.LongFilter;"))
        );
    }

    @Test
    @DisplayName("Should generate LocalDateTime field filter")
    void shouldGenerateLocalDateTimeFieldFilter() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithLocalDateTimeField("com.test.User", "createdAt");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private LocalDateTimeFilter createdAt;")),
                () -> assertTrue(generatedCode.contains("public LocalDateTimeFilter getCreatedAt()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setCreatedAt(LocalDateTimeFilter createdAt)")),
                () -> assertTrue(generatedCode.contains("import com.thy.fss.common.inmemory.filter.LocalDateTimeFilter;"))
        );
    }

    @Test
    @DisplayName("Should generate Instant field filter")
    void shouldGenerateInstantFieldFilter() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithInstantField("com.test.User", "lastModified");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private InstantFilter lastModified;")),
                () -> assertTrue(generatedCode.contains("public InstantFilter getLastModified()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setLastModified(InstantFilter lastModified)")),
                () -> assertTrue(generatedCode.contains("import com.thy.fss.common.inmemory.filter.InstantFilter;"))
        );
    }

    @Test
    @DisplayName("Should skip final fields")
    void shouldSkipFinalFields() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithFinalField("com.test.User", "CONSTANT");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertFalse(generatedCode.contains("CONSTANT"));
    }

    @Test
    @DisplayName("Should generate builder methods for string fields")
    void shouldGenerateBuilderMethodsForStringFields() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStringField("com.test.User", "name");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder name()")),
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder nameEquals(String value)")),
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder nameContains(String value)")),
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder nameStartsWith(String value)")),
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder nameEndsWith(String value)")),
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder nameIsEmpty(Boolean value)")),
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder nameIsBlank(Boolean value)"))
        );
    }

    @Test
    @DisplayName("Should generate builder methods for integer fields")
    void shouldGenerateBuilderMethodsForIntegerFields() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithIntegerField("com.test.User", "age");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder age()")),
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder ageEquals(Integer value)")),
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder ageGreaterThan(Integer value)")),
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder ageLessThan(Integer value)")),
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder ageGreaterOrEqualThan(Integer value)")),
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder ageLessOrEqualThan(Integer value)"))
        );
    }

    @Test
    @DisplayName("Should generate builder methods for collection fields")
    void shouldGenerateBuilderMethodsForCollectionFields() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithCollectionField("com.test.User", "tags");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder tags(CollectionFilter<java.lang.String> filter)")),
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder tagsContains(java.lang.String value)")),
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder tagsIsEmpty(Boolean value)")),
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder tagsIsNotEmpty(Boolean value)"))
        );
    }

    @Test
    @DisplayName("Should generate generic builder class")
    void shouldGenerateGenericBuilderClass() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStringField("com.test.User", "name");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public static class UserFilterGenericBuilder<P>")),
                () -> assertTrue(generatedCode.contains("private final P parent;")),
                () -> assertTrue(generatedCode.contains("private final UserFilter filter;")),
                () -> assertTrue(generatedCode.contains("private final Function<P, ?> buildFunction;")),
                () -> assertTrue(generatedCode.contains("public P and()")),
                () -> assertTrue(generatedCode.contains("public Object build()"))
        );
    }

    @Test
    @DisplayName("Should generate equals method")
    void shouldGenerateEqualsMethod() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStringField("com.test.User", "name");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("@Override")),
                () -> assertTrue(generatedCode.contains("public boolean equals(Object o)")),
                () -> assertTrue(generatedCode.contains("if (this == o) return true;")),
                () -> assertTrue(generatedCode.contains("if (o == null || getClass() != o.getClass()) return false;")),
                () -> assertTrue(generatedCode.contains("Objects.equals(name, that.name)"))
        );
    }

    @Test
    @DisplayName("Should generate hashCode method")
    void shouldGenerateHashCodeMethod() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStringField("com.test.User", "name");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("@Override")),
                () -> assertTrue(generatedCode.contains("public int hashCode()")),
                () -> assertTrue(generatedCode.contains("Objects.hash(name)"))
        );
    }

    @ParameterizedTest
    @MethodSource("provideBuilderTestCases")
    @DisplayName("Should generate proper builder methods")
    void shouldGenerateProperBuilderMethods(String testName, List<String> expectedStrings) throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStringField("com.test.User", "name");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                expectedStrings.stream()
                        .map(expected -> (Executable) () -> assertTrue(generatedCode.contains(expected),
                                "Expected code to contain: " + expected))
                        .toArray(Executable[]::new)
        );

    }

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> provideBuilderTestCases() {
        return java.util.stream.Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        "null checks",
                        List.of(
                                "if (filter == null) {",
                                "return; // Create empty filter when input is null",
                                "this.name = filter.name != null ?"
                        )
                ),
                org.junit.jupiter.params.provider.Arguments.of(
                        "method chaining",
                        List.of(
                                "return this;",
                                "public UserFilterBuilder nameEquals(String value) {",
                                "return this;"
                        )
                )
        );
    }

    @Test
    @DisplayName("Should generate toString method")
    void shouldGenerateToStringMethod() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStringField("com.test.User", "name");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("@Override")),
                () -> assertTrue(generatedCode.contains("public String toString()")),
                () -> assertTrue(generatedCode.contains("return \"UserFilter{\" +")),
                () -> assertTrue(generatedCode.contains("\"name=\" + name"))
        );
    }

    @Test
    @DisplayName("Should generate proper copy constructor for collection fields")
    void shouldGenerateProperCopyConstructorForCollectionFields() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithCollectionField("com.test.User", "tags");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public UserFilter(UserFilter filter)")),
                () -> assertTrue(generatedCode.contains("this.tags = filter.tags != null ?")),
                () -> assertTrue(generatedCode.contains("new CollectionFilter<>(filter.tags) : null;"))
        );
    }

    @Test
    @DisplayName("Should generate proper copy constructor for nested model fields")
    void shouldGenerateProperCopyConstructorForNestedModelFields() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithNestedModelField("com.test.User", "profile");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public UserFilter(UserFilter filter)")),
                () -> assertTrue(generatedCode.contains("this.profile = filter.profile != null ?")),
                () -> assertTrue(generatedCode.contains("new ProfileFilter(filter.profile) : null;"))
        );
    }

    @Test
    @DisplayName("Should handle primitive int field")
    void shouldHandlePrimitiveIntField() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithPrimitiveIntField("com.test.User", "count");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private IntegerFilter count;")),
                () -> assertTrue(generatedCode.contains("public IntegerFilter getCount()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setCount(IntegerFilter count)"))
        );
    }

    @Test
    @DisplayName("Should handle primitive boolean field")
    void shouldHandlePrimitiveBooleanField() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithPrimitiveBooleanField("com.test.User", "enabled");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private BooleanFilter enabled;")),
                () -> assertTrue(generatedCode.contains("public BooleanFilter getEnabled()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setEnabled(BooleanFilter enabled)"))
        );
    }

    @Test
    @DisplayName("Should handle primitive long field")
    void shouldHandlePrimitiveLongField() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithPrimitiveLongField("com.test.User", "timestamp");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private LongFilter timestamp;")),
                () -> assertTrue(generatedCode.contains("public LongFilter getTimestamp()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setTimestamp(LongFilter timestamp)"))
        );
    }

    @Test
    @DisplayName("Should handle primitive double field")
    void shouldHandlePrimitiveDoubleField() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithPrimitiveDoubleField("com.test.User", "score");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private DoubleFilter score;")),
                () -> assertTrue(generatedCode.contains("public DoubleFilter getScore()")),
                () -> assertTrue(generatedCode.contains("public UserFilter setScore(DoubleFilter score)"))
        );
    }


    @Test
    @DisplayName("Should generate proper package extraction")
    void shouldGenerateProperPackageExtraction() throws Exception {
        // Given - Test with deeply nested package
        TypeElement typeElement = createSimpleTypeElement("com.example.deep.nested.package.TestClass");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertTrue(generatedCode.contains("package com.example.deep.nested.package;"));
    }

    @Test
    @DisplayName("Should handle class with mixed field types")
    void shouldHandleClassWithMixedFieldTypes() throws Exception {
        // Given
        TypeElement typeElement = createMixedFieldTypeElement();

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("private StringFilter name;")),
                () -> assertTrue(generatedCode.contains("private IntegerFilter age;")),
                () -> assertTrue(generatedCode.contains("private BooleanFilter active;")),
                () -> assertTrue(generatedCode.contains("private LocalDateFilter birthDate;")),
                () -> assertTrue(generatedCode.contains("private CollectionFilter<java.lang.String> tags;")),
                () -> assertTrue(generatedCode.contains("private ProfileFilter profile;"))
        );
    }

    @Test
    @DisplayName("Should generate proper field validation in copy constructor")
    void shouldGenerateProperFieldValidationInCopyConstructor() throws Exception {
        // Given
        TypeElement typeElement = createMixedFieldTypeElement();

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public UserFilter(UserFilter filter)")),
                () -> assertTrue(generatedCode.contains("this.name = filter.name != null ?")),
                () -> assertTrue(generatedCode.contains("this.age = filter.age != null ?")),
                () -> assertTrue(generatedCode.contains("this.active = filter.active != null ?"))
        );
    }

    @Test
    @DisplayName("Should generate proper generic builder functionality")
    void shouldGenerateProperGenericBuilderFunctionality() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithStringField("com.test.User", "name");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public static class UserFilterGenericBuilder<P>")),
                () -> assertTrue(generatedCode.contains("private final P parent;")),
                () -> assertTrue(generatedCode.contains("private final UserFilter filter;")),
                () -> assertTrue(generatedCode.contains("private final Function<P, ?> buildFunction;")),
                () -> assertTrue(generatedCode.contains("public P and() {")),
                () -> assertTrue(generatedCode.contains("return parent;"))
        );
    }

    @Test
    @DisplayName("Should generate nested builder methods for nested model fields")
    void shouldGenerateNestedBuilderMethodsForNestedModelFields() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithNestedModelField("com.test.User", "profile");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public UserFilterBuilder profile(ProfileFilter profileFilter)")),
                () -> assertTrue(generatedCode.contains("public ProfileFilter.ProfileFilterGenericBuilder<UserFilterBuilder> profile()"))
        );
    }

    @Test
    @DisplayName("Should generate all required imports")
    void shouldGenerateAllRequiredImports() throws Exception {
        // Given
        TypeElement typeElement = createComplexTypeElement();

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("import java.util.Objects;")),
                () -> assertTrue(generatedCode.contains("import java.util.function.Function;")),
                () -> assertTrue(generatedCode.contains("import java.time.LocalDate;")),
                () -> assertTrue(generatedCode.contains("import java.time.LocalDateTime;")),
                () -> assertTrue(generatedCode.contains("import java.time.Instant;")),
                () -> assertTrue(generatedCode.contains("import com.thy.fss.common.inmemory.filter.Filter;"))
        );
    }

    @Test
    @DisplayName("Should handle empty class with no fields")
    void shouldHandleEmptyClassWithNoFields() throws Exception {
        // Given
        TypeElement typeElement = createSimpleTypeElement("com.test.EmptyClass");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertAll(
                () -> assertTrue(generatedCode.contains("public class EmptyClassFilter implements EntityFilter<EmptyClass>")),
                () -> assertTrue(generatedCode.contains("public EmptyClassFilter()")),
                () -> assertTrue(generatedCode.contains("public EmptyClassFilter(EmptyClassFilter filter)")),
                () -> assertTrue(generatedCode.contains("public static EmptyClassFilterBuilder builder()"))
        );
    }

    // Helper methods
    private TypeElement createSimpleTypeElement(String qualifiedName) {
        TypeElement typeElement = mock(TypeElement.class);
        Name qualifiedNameObj = mock(Name.class);
        Name simpleNameObj = mock(Name.class);

        String simpleName = qualifiedName.contains(".") ?
                qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1) : qualifiedName;

        lenient().when(qualifiedNameObj.toString()).thenReturn(qualifiedName);
        lenient().when(simpleNameObj.toString()).thenReturn(simpleName);
        lenient().when(typeElement.getQualifiedName()).thenReturn(qualifiedNameObj);
        lenient().when(typeElement.getSimpleName()).thenReturn(simpleNameObj);
        lenient().when(typeElement.getEnclosedElements()).thenReturn(Collections.emptyList());

        return typeElement;
    }

    private TypeElement createTypeElementWithStringField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createStringField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithIntegerField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createIntegerField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithBooleanField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createBooleanField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithLocalDateField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createLocalDateField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithStaticField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createStaticField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private TypeElement createComplexTypeElement() {
        TypeElement typeElement = createSimpleTypeElement("com.test.User");

        List<Element> fields = List.of(
                createStringField("name"),
                createIntegerField("age"),
                createBooleanField("active")
        );

        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) fields);
        return typeElement;
    }

    private VariableElement createStringField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.lang.String");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);

        return field;
    }

    private VariableElement createIntegerField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.lang.Integer");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);

        return field;
    }

    private VariableElement createBooleanField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.lang.Boolean");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);

        return field;
    }

    private VariableElement createLocalDateField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.time.LocalDate");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);

        return field;
    }

    private VariableElement createStaticField(String fieldName) {
        VariableElement field = createStringField(fieldName);
        lenient().when(field.getModifiers()).thenReturn(Set.of(Modifier.STATIC));
        return field;
    }

    private VariableElement createEnumField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        TypeElement enumElement = mock(TypeElement.class);
        Name enumQualifiedName = mock(Name.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("com.test.Status");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(enumElement);
        lenient().when(enumElement.getKind()).thenReturn(ElementKind.ENUM);
        lenient().when(enumElement.getQualifiedName()).thenReturn(enumQualifiedName);
        lenient().when(enumQualifiedName.toString()).thenReturn("com.test.Status");

        return field;
    }

    private TypeElement createTypeElementWithEnumField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createEnumField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private VariableElement createCollectionField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        TypeElement collectionElement = mock(TypeElement.class);
        Name collectionQualifiedName = mock(Name.class);
        TypeMirror stringType = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.util.List<java.lang.String>");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(collectionElement);
        lenient().when(collectionElement.getQualifiedName()).thenReturn(collectionQualifiedName);
        lenient().when(collectionQualifiedName.toString()).thenReturn("java.util.List");
        lenient().when(type.getTypeArguments()).thenReturn((List) Collections.singletonList(stringType));
        lenient().when(stringType.toString()).thenReturn("java.lang.String");

        return field;
    }

    private TypeElement createTypeElementWithCollectionField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createCollectionField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private VariableElement createNestedModelField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        TypeElement nestedElement = mock(TypeElement.class);
        Name nestedSimpleName = mock(Name.class);
        Name nestedQualifiedName = mock(Name.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("com.test.Profile");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.asElement()).thenReturn(nestedElement);
        lenient().when(nestedElement.getSimpleName()).thenReturn(nestedSimpleName);
        lenient().when(nestedSimpleName.toString()).thenReturn("Profile");
        lenient().when(nestedElement.getQualifiedName()).thenReturn(nestedQualifiedName);
        lenient().when(nestedQualifiedName.toString()).thenReturn("com.test.Profile");
        lenient().when(nestedElement.getAnnotation(MetaModel.class)).thenReturn(mock(MetaModel.class));

        return field;
    }

    private TypeElement createTypeElementWithNestedModelField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createNestedModelField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private VariableElement createDoubleField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.lang.Double");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);

        return field;
    }

    private TypeElement createTypeElementWithDoubleField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createDoubleField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private VariableElement createLongField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.lang.Long");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);

        return field;
    }

    private TypeElement createTypeElementWithLongField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createLongField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private VariableElement createLocalDateTimeField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.time.LocalDateTime");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);

        return field;
    }

    private TypeElement createTypeElementWithLocalDateTimeField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createLocalDateTimeField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private VariableElement createInstantField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("java.time.Instant");
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);

        return field;
    }

    private TypeElement createTypeElementWithInstantField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createInstantField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private VariableElement createFinalField(String fieldName) {
        VariableElement field = createStringField(fieldName);
        lenient().when(field.getModifiers()).thenReturn(Set.of(Modifier.FINAL));
        return field;
    }

    private TypeElement createTypeElementWithFinalField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createFinalField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private VariableElement createPrimitiveIntField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("int");
        lenient().when(type.getKind()).thenReturn(TypeKind.INT);

        return field;
    }

    private TypeElement createTypeElementWithPrimitiveIntField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createPrimitiveIntField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private VariableElement createPrimitiveBooleanField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("boolean");
        lenient().when(type.getKind()).thenReturn(TypeKind.BOOLEAN);

        return field;
    }

    private TypeElement createTypeElementWithPrimitiveBooleanField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createPrimitiveBooleanField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private VariableElement createPrimitiveLongField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("long");
        lenient().when(type.getKind()).thenReturn(TypeKind.LONG);

        return field;
    }

    private TypeElement createTypeElementWithPrimitiveLongField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createPrimitiveLongField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }

    private VariableElement createPrimitiveDoubleField(String fieldName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        TypeMirror type = mock(TypeMirror.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(type.toString()).thenReturn("double");
        lenient().when(type.getKind()).thenReturn(TypeKind.DOUBLE);

        return field;
    }

    private TypeElement createTypeElementWithPrimitiveDoubleField(String qualifiedName, String fieldName) {
        TypeElement typeElement = createSimpleTypeElement(qualifiedName);
        VariableElement field = createPrimitiveDoubleField(fieldName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) Collections.singletonList(field));
        return typeElement;
    }


    private TypeElement createMixedFieldTypeElement() {
        TypeElement typeElement = createSimpleTypeElement("com.test.User");

        List<Element> fields = List.of(
                createStringField("name"),
                createIntegerField("age"),
                createBooleanField("active"),
                createLocalDateField("birthDate"),
                createCollectionField("tags"),
                createNestedModelField("profile")
        );

        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) fields);
        return typeElement;
    }
}