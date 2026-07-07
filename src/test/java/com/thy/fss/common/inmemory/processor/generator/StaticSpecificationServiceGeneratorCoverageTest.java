package com.thy.fss.common.inmemory.processor.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("StaticSpecificationServiceGenerator Coverage Tests")
class StaticSpecificationServiceGeneratorCoverageTest {

    @Mock
    private ProcessingEnvironment processingEnv;

    @Mock
    private Messager messager;

    @Mock
    private Filer filer;

    @Mock
    private JavaFileObject javaFileObject;

    @Mock
    private Elements elementUtils;

    private StringWriter stringWriter;
    private StaticSpecificationServiceGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        stringWriter = new StringWriter();

        lenient().when(processingEnv.getMessager()).thenReturn(messager);
        lenient().when(processingEnv.getFiler()).thenReturn(filer);
        lenient().when(processingEnv.getElementUtils()).thenReturn(elementUtils);
        lenient().when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        lenient().when(javaFileObject.openWriter()).thenReturn(stringWriter);

        generator = new StaticSpecificationServiceGenerator(processingEnv);
    }

    @Test
    @DisplayName("Should generate model type collection validation helper methods")
    void shouldGenerateModelTypeCollectionValidationHelperMethods() throws Exception {
        TypeElement userType = createTypeElementWithModelCollectionField();

        generator.generate(userType);

        String generatedCode = stringWriter.toString();

        assertThat(generatedCode)
                .contains("validateMembersCollectionAnyWithModelFilter")
                .contains("validateMembersCollectionAllWithModelFilter")
                .contains("validateMembersCollectionNoneWithModelFilter")
                .contains("validateCollectionElement(element, elementFilter, elementService)")
                .contains("MemberSpecificationService elementService");
    }

    @Test
    @DisplayName("Should generate model type collection validation delegation in validateFilter")
    void shouldGenerateModelTypeCollectionValidationDelegationInValidateFilter() throws Exception {
        TypeElement userType = createTypeElementWithModelCollectionField();

        generator.generate(userType);

        String generatedCode = stringWriter.toString();

        assertThat(generatedCode)
                .contains("// Model type collection - delegate to element type service")
                .contains("if (teamFilter.getMembers().getCollectionAny() != null)")
                .contains("if (!validateMembersCollectionAnyWithModelFilter(entity, modelFilter, MemberSpecificationService.INSTANCE)) return false;")
                .contains("if (teamFilter.getMembers().getCollectionAll() != null)")
                .contains("if (!validateMembersCollectionAllWithModelFilter(entity, modelFilter, MemberSpecificationService.INSTANCE)) return false;")
                .contains("if (teamFilter.getMembers().getCollectionNone() != null)")
                .contains("if (!validateMembersCollectionNoneWithModelFilter(entity, modelFilter, MemberSpecificationService.INSTANCE)) return false;");
    }

    private TypeElement createTypeElementWithModelCollectionField() {
        TypeElement typeElement = mock(TypeElement.class);
        Name qualifiedName = mock(Name.class);
        Name simpleName = mock(Name.class);

        lenient().when(qualifiedName.toString()).thenReturn("com.test.Team");
        lenient().when(simpleName.toString()).thenReturn("Team");
        lenient().when(typeElement.getQualifiedName()).thenReturn(qualifiedName);
        lenient().when(typeElement.getSimpleName()).thenReturn(simpleName);

        VariableElement membersField = createModelCollectionField("members", "com.test.Member");
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) List.of(membersField));

        TypeElement modelType = mock(TypeElement.class);
        lenient().when(modelType.getKind()).thenReturn(ElementKind.CLASS);
        lenient().when(elementUtils.getTypeElement("com.test.Member")).thenReturn(modelType);

        return typeElement;
    }

    private VariableElement createModelCollectionField(String fieldName, String elementTypeName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType collectionType = mock(DeclaredType.class);
        DeclaredType listElement = mock(DeclaredType.class);
        Element collectionElement = mock(Element.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());

        lenient().when(field.asType()).thenReturn(collectionType);
        lenient().when(collectionType.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(collectionType.toString()).thenReturn("java.util.List<" + elementTypeName + ">");
        lenient().when(collectionType.asElement()).thenReturn(collectionElement);
        lenient().when(collectionElement.toString()).thenReturn("java.util.List");

        lenient().when(listElement.toString()).thenReturn(elementTypeName);
        lenient().when(listElement.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(collectionType.getTypeArguments()).thenReturn((List) List.of(listElement));

        TypeElement enclosingType = mock(TypeElement.class);
        lenient().when(field.getEnclosingElement()).thenReturn(enclosingType);
        lenient().when(enclosingType.getInterfaces()).thenReturn(Collections.emptyList());

        return field;
    }
}
