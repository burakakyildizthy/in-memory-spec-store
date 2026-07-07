package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.processor.model.DateTimeFormatInfo;
import com.thy.fss.common.inmemory.processor.model.FilterFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("FilterDeserializerGenerator Coverage Tests")
class FilterDeserializerGeneratorCoverageTest {

    @Mock
    private ProcessingEnvironment processingEnv;

    @Mock
    private Filer filer;

    @Mock
    private Messager messager;

    @Mock
    private Elements elements;

    @Mock
    private Types types;

    @Mock
    private JavaFileObject javaFileObject;

    private StringWriter stringWriter;
    private FilterDeserializerGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        stringWriter = new StringWriter();
        lenient().when(processingEnv.getFiler()).thenReturn(filer);
        lenient().when(processingEnv.getMessager()).thenReturn(messager);
        lenient().when(processingEnv.getElementUtils()).thenReturn(elements);
        lenient().when(processingEnv.getTypeUtils()).thenReturn(types);
        lenient().when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        lenient().when(javaFileObject.openWriter()).thenReturn(stringWriter);

        generator = new FilterDeserializerGenerator(processingEnv);
    }

    @Test
    @DisplayName("Should generate mixed field type operators through public generateDeserializer flow")
    void shouldGenerateMixedFieldTypeOperators() throws Exception {
        FilterFieldConfig stringConfig = new FilterFieldConfig("name", "java.lang.String", "StringFilter");
        stringConfig.setString(true);

        FilterFieldConfig numericConfig = new FilterFieldConfig("age", "java.lang.Integer", "IntegerFilter");
        numericConfig.setNumeric(true);

        FilterFieldConfig temporalConfig = new FilterFieldConfig("createdAt", "java.time.LocalDateTime", "LocalDateTimeFilter");
        temporalConfig.setTemporal(true);
        temporalConfig.setDateTimeFormatInfo(
                DateTimeFormatInfo.withCustomPattern("yyyy-MM-dd'T'HH:mm:ss", "java.time.LocalDateTime")
        );

        generator.generateDeserializer(
                "UserFilter",
                "com.test",
                List.of(stringConfig, numericConfig, temporalConfig),
                "User"
        );

        String generatedCode = stringWriter.toString();

        assertThat(generatedCode)
                .contains("\"name.cont\"")
                .contains("\"name.match\"")
                .contains("\"age.gt\"")
                .contains("\"age.lte\"")
                .contains("\"createdAt.be\"")
                .contains("\"createdAt.next\"")
                .contains("deserializer.deserializeTemporalPreset(paramValue)");
    }

    @Test
    @DisplayName("Should generate nested collection operator bindings for String elements")
    void shouldGenerateNestedCollectionOperatorBindingsForStringElements() throws Exception {
        FilterFieldConfig tagsConfig = new FilterFieldConfig(
                "tags",
                "java.util.Collection<java.lang.String>",
                "CollectionFilter"
        );
        tagsConfig.setCollection(true);
        tagsConfig.setElementType("java.lang.String");

        generator.generateDeserializer(
                "UserFilter",
                "com.test",
                List.of(tagsConfig),
                "User"
        );

        String generatedCode = stringWriter.toString();

        assertThat(generatedCode)
                .contains("case \"tags.any.cont\"")
                .contains("case \"tags.all.start\"")
                .contains("case \"tags.none.end\"")
                .contains("case \"tags.any.match\"")
                .contains("case \"tags.cont\"")
                .contains("setCollectionContains(value)");
    }

    @Test
    @DisplayName("Should generate handleNestedFilterPath for model type collection fields")
    void shouldGenerateHandleNestedFilterPathForModelTypeCollections() throws Exception {
        FilterFieldConfig usersConfig = new FilterFieldConfig(
                "users",
                "java.util.Collection<com.test.User>",
                "CollectionFilter"
        );
        usersConfig.setCollection(true);
        usersConfig.setElementType("com.test.User");

        generator.generateDeserializer(
                "GroupFilter",
                "com.test",
                List.of(usersConfig),
                "Group"
        );

        String generatedCode = stringWriter.toString();

        assertThat(generatedCode)
                .contains("private static void handleNestedFilterPath(")
                .contains("if (mappedPath.startsWith(\"users.any.\"))")
                .contains("if (mappedPath.startsWith(\"users.all.\"))")
                .contains("if (mappedPath.startsWith(\"users.none.\"))")
                .contains("UserFilterDeserializer.bindParameter(")
                .contains("not found on filter type 'GroupFilter'");
    }
}
