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

/**
 * Tests for cross-package import generation in StaticSpecificationServiceGenerator.
 * 
 * When a metamodel has fields that reference other metamodels (sub-models) from different
 * packages, the generated SpecificationService class must include proper imports for:
 * - The entity class itself
 * - The Filter class
 * - The SpecificationService class
 * - The static metamodel class (_)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cross-Package Import Generation Tests")
class CrossPackageImportGenerationTest {

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
    @DisplayName("Should generate cross-package imports for model type field in different package")
    void shouldGenerateCrossPackageImportsForModelTypeField() throws Exception {
        // Given: Parent model in com.example.parent, dependent model in com.example.sub
        TypeElement typeElement = createTypeElementWithCrossPackageModelField(
                "com.example.parent.Order", "customer", "com.example.sub.Customer");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertThat(generatedCode)
                .contains("package com.example.parent;")
                .contains("import com.example.sub.Customer;")
                .contains("import com.example.sub.CustomerFilter;")
                .contains("import com.example.sub.Customer_;")
                .contains("import com.example.sub.CustomerSpecificationService;");
    }

    @Test
    @DisplayName("Should not generate redundant imports for model type in same package")
    void shouldNotGenerateRedundantImportsForSamePackageModel() throws Exception {
        // Given: Both models in com.test package
        TypeElement typeElement = createTypeElementWithCrossPackageModelField(
                "com.test.Order", "profile", "com.test.Profile");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        // Should import SpecificationService (always needed)
        assertThat(generatedCode).contains("import com.test.ProfileSpecificationService;");
        // Should NOT have explicit entity/filter/metamodel imports since they are in the same package
        assertThat(generatedCode).doesNotContain("import com.test.Profile;");
        assertThat(generatedCode).doesNotContain("import com.test.ProfileFilter;");
        assertThat(generatedCode).doesNotContain("import com.test.Profile_;");
    }

    @Test
    @DisplayName("Should generate cross-package imports for collection element model type in different package")
    void shouldGenerateCrossPackageImportsForCollectionElementModelType() throws Exception {
        // Given: Parent model in com.example.parent, collection element model in com.example.sub
        TypeElement typeElement = createTypeElementWithCrossPackageCollectionField(
                "com.example.parent.Department", "employees", "com.example.sub.Employee");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertThat(generatedCode)
                .contains("package com.example.parent;")
                .contains("import com.example.sub.Employee;")
                .contains("import com.example.sub.EmployeeFilter;")
                .contains("import com.example.sub.Employee_;")
                .contains("import com.example.sub.EmployeeSpecificationService;");
    }

    @Test
    @DisplayName("Should not generate redundant imports for collection element model in same package")
    void shouldNotGenerateRedundantImportsForSamePackageCollectionElement() throws Exception {
        // Given: Both models in com.test package
        TypeElement typeElement = createTypeElementWithCrossPackageCollectionField(
                "com.test.Team", "members", "com.test.Member");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        // Should import SpecificationService
        assertThat(generatedCode).contains("import com.test.MemberSpecificationService;");
        // Should NOT have explicit entity/filter/metamodel imports since they are in the same package
        assertThat(generatedCode).doesNotContain("import com.test.Member;");
        assertThat(generatedCode).doesNotContain("import com.test.MemberFilter;");
        assertThat(generatedCode).doesNotContain("import com.test.Member_;");
    }

    @Test
    @DisplayName("Should generate cross-package imports for multiple dependent types from different packages")
    void shouldGenerateCrossPackageImportsForMultipleDependentTypes() throws Exception {
        // Given: Parent model with fields from multiple different packages
        TypeElement typeElement = createTypeElementWithMultipleCrossPackageFields(
                "com.example.parent.Order",
                "customer", "com.example.customers.Customer",
                "items", "com.example.products.Product");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        // Verify imports for Customer (model field)
        assertThat(generatedCode)
                .contains("import com.example.customers.Customer;")
                .contains("import com.example.customers.CustomerFilter;")
                .contains("import com.example.customers.Customer_;")
                .contains("import com.example.customers.CustomerSpecificationService;");

        // Verify imports for Product (collection element field)
        assertThat(generatedCode)
                .contains("import com.example.products.Product;")
                .contains("import com.example.products.ProductFilter;")
                .contains("import com.example.products.Product_;")
                .contains("import com.example.products.ProductSpecificationService;");
    }

    @Test
    @DisplayName("Should generate cross-package imports and use entity class in generated code")
    void shouldUseCrossPackageEntityClassInGeneratedCode() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithCrossPackageCollectionField(
                "com.example.parent.Department", "employees", "com.example.sub.Employee");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        // The generated code should reference the simple name (relying on imports)
        assertThat(generatedCode)
                .contains("EmployeeFilter")
                .contains("EmployeeSpecificationService");
    }

    @Test
    @DisplayName("Should generate cross-package imports for deeply nested packages")
    void shouldGenerateCrossPackageImportsForDeeplyNestedPackages() throws Exception {
        // Given
        TypeElement typeElement = createTypeElementWithCrossPackageModelField(
                "com.example.module.a.deep.Entity",
                "reference",
                "com.example.module.b.deep.Referenced");

        // When
        generator.generate(typeElement);

        // Then
        String generatedCode = stringWriter.toString();

        assertThat(generatedCode)
                .contains("package com.example.module.a.deep;")
                .contains("import com.example.module.b.deep.Referenced;")
                .contains("import com.example.module.b.deep.ReferencedFilter;")
                .contains("import com.example.module.b.deep.Referenced_;")
                .contains("import com.example.module.b.deep.ReferencedSpecificationService;");
    }

    // ==================== Helper methods ====================

    private TypeElement createTypeElementWithCrossPackageModelField(
            String parentQualifiedName, String fieldName, String fieldTypeQualifiedName) {
        TypeElement typeElement = createSimpleTypeElement(parentQualifiedName);
        VariableElement field = createModelField(fieldName, fieldTypeQualifiedName, typeElement);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithCrossPackageCollectionField(
            String parentQualifiedName, String fieldName, String elementTypeQualifiedName) {
        TypeElement typeElement = createSimpleTypeElement(parentQualifiedName);
        VariableElement field = createModelCollectionField(fieldName, elementTypeQualifiedName, typeElement);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));
        return typeElement;
    }

    private TypeElement createTypeElementWithMultipleCrossPackageFields(
            String parentQualifiedName,
            String modelFieldName, String modelFieldType,
            String collectionFieldName, String collectionElementType) {
        TypeElement typeElement = createSimpleTypeElement(parentQualifiedName);
        VariableElement modelField = createModelField(modelFieldName, modelFieldType, typeElement);
        VariableElement collectionField = createModelCollectionField(
                collectionFieldName, collectionElementType, typeElement);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) List.of(modelField, collectionField));
        return typeElement;
    }

    private TypeElement createSimpleTypeElement(String qualifiedName) {
        TypeElement typeElement = mock(TypeElement.class);
        Name qualifiedNameObj = mock(Name.class);
        Name simpleNameObj = mock(Name.class);

        String simpleName = qualifiedName.contains(".")
                ? qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1)
                : qualifiedName;

        lenient().when(qualifiedNameObj.toString()).thenReturn(qualifiedName);
        lenient().when(simpleNameObj.toString()).thenReturn(simpleName);
        lenient().when(typeElement.getQualifiedName()).thenReturn(qualifiedNameObj);
        lenient().when(typeElement.getSimpleName()).thenReturn(simpleNameObj);
        lenient().when(typeElement.getEnclosedElements()).thenReturn(Collections.emptyList());

        return typeElement;
    }

    private VariableElement createModelField(String fieldName, String fieldTypeQualifiedName,
                                             TypeElement enclosingType) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType type = mock(DeclaredType.class);
        TypeElement fieldTypeElement = mock(TypeElement.class);
        Name fieldTypeQualName = mock(Name.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(type);
        lenient().when(field.getEnclosingElement()).thenReturn(enclosingType);

        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.toString()).thenReturn(fieldTypeQualifiedName);
        lenient().when(type.asElement()).thenReturn(fieldTypeElement);

        lenient().when(fieldTypeElement.toString()).thenReturn(fieldTypeQualifiedName);
        lenient().when(fieldTypeElement.getKind()).thenReturn(ElementKind.CLASS);
        lenient().when(fieldTypeQualName.toString()).thenReturn(fieldTypeQualifiedName);
        lenient().when(fieldTypeElement.getQualifiedName()).thenReturn(fieldTypeQualName);

        lenient().when(enclosingType.getInterfaces()).thenReturn(Collections.emptyList());

        return field;
    }

    private VariableElement createModelCollectionField(String fieldName, String elementTypeName,
                                                       TypeElement enclosingType) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        DeclaredType collectionType = mock(DeclaredType.class);
        DeclaredType listElement = mock(DeclaredType.class);
        Element collectionElement = mock(Element.class);
        TypeElement elementTypeElement = mock(TypeElement.class);
        Name elementTypeQualName = mock(Name.class);

        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());
        lenient().when(field.asType()).thenReturn(collectionType);
        lenient().when(field.getEnclosingElement()).thenReturn(enclosingType);

        lenient().when(collectionType.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(collectionType.toString()).thenReturn("java.util.List<" + elementTypeName + ">");
        lenient().when(collectionType.asElement()).thenReturn(collectionElement);
        lenient().when(collectionElement.toString()).thenReturn("java.util.List");

        lenient().when(listElement.toString()).thenReturn(elementTypeName);
        lenient().when(listElement.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(listElement.asElement()).thenReturn(elementTypeElement);
        lenient().when(collectionType.getTypeArguments()).thenReturn((List) List.of(listElement));

        lenient().when(elementTypeElement.toString()).thenReturn(elementTypeName);
        lenient().when(elementTypeElement.getKind()).thenReturn(ElementKind.CLASS);
        lenient().when(elementTypeQualName.toString()).thenReturn(elementTypeName);
        lenient().when(elementTypeElement.getQualifiedName()).thenReturn(elementTypeQualName);

        lenient().when(enclosingType.getInterfaces()).thenReturn(Collections.emptyList());

        return field;
    }
}
