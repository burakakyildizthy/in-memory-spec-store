package com.thy.fss.common.inmemory.processor.generator.importbug;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.thy.fss.common.inmemory.processor.MetaModel;
import com.thy.fss.common.inmemory.processor.generator.FilterMetaModelGenerator;
import com.thy.fss.common.inmemory.processor.generator.StaticMetaModelGenerator;

/**
 * Unit tests for collectTypeImports behavior with deterministic assertions for each field shape.
 *
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
 *
 * Tests the import collection method behavior with specific field shapes:
 * - Direct user type
 * - List<Order>, Set<Order>, List<List<Order>>
 * - Cross-package resolution by fully qualified name
 * - Standard-type omission
 * - Filter-specific derived ...Filter imports
 *
 * Note: Map-typed field shapes are intentionally not exercised here. Map is not a
 * supported/covered field type for this generator, so Map-specific tests have been removed.
 */
@DisplayName("Import Collection Unit Tests")
class ImportCollectionUnitTest {

    private static final String ORDER_QUALIFIED = "com.other.pkg.Order";
    private static final String STATUS_QUALIFIED = "com.other.pkg.Status";
    private static final String CONTAINER_QUALIFIED = "com.test.TestEntity";

    // ==================== Static Meta Model Tests ====================

    @Nested
    @DisplayName("Static Meta Model - Import Collection")
    class StaticMetaModelImports {

        @Test
        @DisplayName("Direct user type field imports its qualified name")
        void directUserType_importsQualifiedName() throws Exception {
            VariableElement field = buildDirectModelField("order", ORDER_QUALIFIED, "Order", true);
            String generated = generateStaticMetaModel(field);

            assertThat(generated).contains("import com.other.pkg.Order;");
        }

        @Test
        @DisplayName("List<Order> imports Order")
        void listOrder_importsOrder() throws Exception {
            VariableElement field = buildCollectionField("orders", "java.util.List", ORDER_QUALIFIED, "Order", true);
            String generated = generateStaticMetaModel(field);

            assertThat(generated).contains("import com.other.pkg.Order;");
            assertThat(generated).contains("import java.util.Collection;");
        }

        @Test
        @DisplayName("Set<Order> imports Order")
        void setOrder_importsOrder() throws Exception {
            VariableElement field = buildCollectionField("orders", "java.util.Set", ORDER_QUALIFIED, "Order", true);
            String generated = generateStaticMetaModel(field);

            assertThat(generated).contains("import com.other.pkg.Order;");
        }

        @Test
        @DisplayName("List<List<Order>> imports Order from doubly-nested collection")
        void listListOrder_importsOrder() throws Exception {
            DeclaredType innerList = buildListDeclaredType(createUserType(ORDER_QUALIFIED, "Order", true));
            VariableElement field = buildCollectionFieldWithElementType("nestedOrders", "java.util.List", innerList);
            String generated = generateStaticMetaModel(field);

            assertThat(generated).contains("import com.other.pkg.Order;");
        }
    }

    // ==================== Standard Type Omission Tests ====================

    @Nested
    @DisplayName("Standard Type Omission")
    class StandardTypeOmission {

        @Test
        @DisplayName("Primitive wrapper types produce no user-type import")
        void primitiveWrappers_noUserImport() throws Exception {
            VariableElement field = buildStandardTypeField("count", "java.lang.Integer", "Integer");
            String generated = generateStaticMetaModel(field);

            assertThat(generated).doesNotContain("import com.other.pkg");
            assertThat(generated).doesNotContain("import java.lang.Integer;");
        }

        @Test
        @DisplayName("String type produces no user-type import")
        void string_noUserImport() throws Exception {
            VariableElement field = buildStandardTypeField("name", "java.lang.String", "String");
            String generated = generateStaticMetaModel(field);

            assertThat(generated).doesNotContain("import java.lang.String;");
        }

        @Test
        @DisplayName("Temporal types emit java.time import only")
        void temporalTypes_javaTimeOnly() throws Exception {
            VariableElement field = buildStandardTypeField("createdAt", "java.time.LocalDateTime", "LocalDateTime");
            String generated = generateStaticMetaModel(field);

            assertThat(generated).contains("import java.time.LocalDateTime;");
            assertThat(generated).doesNotContain("import com.other.pkg");
        }

        @Test
        @DisplayName("java.util container types are not imported themselves")
        void javaUtilContainers_notImported() throws Exception {
            VariableElement field = buildCollectionField("items", "java.util.List", "java.lang.String", "String", false);
            String generated = generateStaticMetaModel(field);

            assertThat(generated).doesNotContain("import java.util.List;");
            assertThat(generated).doesNotContain("import java.lang.String;");
        }
    }

    // ==================== Filter Meta Model Tests ====================

    @Nested
    @DisplayName("Filter Meta Model - Import Collection")
    class FilterMetaModelImports {

        @Test
        @DisplayName("Direct @MetaModel field emits entity and Filter imports")
        void directMetaModel_emitsEntityAndFilter() throws Exception {
            VariableElement field = buildDirectModelField("order", ORDER_QUALIFIED, "Order", true);
            String generated = generateFilterMetaModel(field);

            assertThat(generated).contains("import com.other.pkg.Order;");
            assertThat(generated).contains("import com.other.pkg.OrderFilter;");
        }

        @Test
        @DisplayName("Direct enum field emits its import")
        void directEnum_emitsImport() throws Exception {
            VariableElement field = buildEnumField("status", STATUS_QUALIFIED, "Status");
            String generated = generateFilterMetaModel(field);

            assertThat(generated).contains("import com.other.pkg.Status;");
        }

        @Test
        @DisplayName("List<List<Order>> in filter emits Order and OrderFilter")
        void listListOrder_emitsFilterImports() throws Exception {
            DeclaredType innerList = buildListDeclaredType(createUserType(ORDER_QUALIFIED, "Order", true));
            VariableElement field = buildCollectionFieldWithElementType("nestedOrders", "java.util.List", innerList);
            String generated = generateFilterMetaModel(field);

            assertThat(generated).contains("import com.other.pkg.Order;");
            assertThat(generated).contains("import com.other.pkg.OrderFilter;");
        }
    }

    // ==================== Cross-Package Resolution Tests ====================

    @Nested
    @DisplayName("Cross-Package Resolution")
    class CrossPackageResolution {

        @Test
        @DisplayName("Types in different package are imported by fully qualified name")
        void differentPackage_fullyQualifiedImport() throws Exception {
            VariableElement field = buildDirectModelField("order", "com.acme.domain.Order", "Order", true);
            String generated = generateStaticMetaModel(field);

            assertThat(generated).contains("import com.acme.domain.Order;");
        }

        @Test
        @DisplayName("Nested collection element type resolved by fully qualified name across packages")
        void nestedCollectionElement_crossPackage() throws Exception {
            VariableElement field = buildCollectionField("products", "java.util.List", "com.acme.domain.Product", "Product", true);
            String generated = generateStaticMetaModel(field);

            assertThat(generated).contains("import com.acme.domain.Product;");
        }
    }

    // ==================== Generator Invocation Helpers ====================

    private String generateStaticMetaModel(VariableElement field) throws Exception {
        ProcessingEnvironment processingEnv = mock(ProcessingEnvironment.class);
        Messager messager = mock(Messager.class);
        Filer filer = mock(Filer.class);
        Elements elementUtils = mock(Elements.class);
        Types typeUtils = mock(Types.class);
        JavaFileObject javaFileObject = mock(JavaFileObject.class);
        StringWriter stringWriter = new StringWriter();

        lenient().when(processingEnv.getMessager()).thenReturn(messager);
        lenient().when(processingEnv.getFiler()).thenReturn(filer);
        lenient().when(processingEnv.getElementUtils()).thenReturn(elementUtils);
        lenient().when(processingEnv.getTypeUtils()).thenReturn(typeUtils);
        lenient().when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        lenient().when(javaFileObject.openWriter()).thenReturn(stringWriter);

        TypeElement collectionTypeElement = mock(TypeElement.class);
        DeclaredType collectionDeclaredType = mock(DeclaredType.class);
        lenient().when(elementUtils.getTypeElement("java.util.Collection")).thenReturn(collectionTypeElement);
        lenient().when(collectionTypeElement.asType()).thenReturn(collectionDeclaredType);
        lenient().when(typeUtils.erasure(any())).thenReturn(collectionDeclaredType);
        lenient().when(typeUtils.isAssignable(any(), any())).thenReturn(false);

        TypeElement typeElement = createContainerTypeElement(field);
        StaticMetaModelGenerator generator = new StaticMetaModelGenerator(processingEnv);
        generator.generate(typeElement);
        return stringWriter.toString();
    }

    private String generateFilterMetaModel(VariableElement field) throws Exception {
        ProcessingEnvironment processingEnv = mock(ProcessingEnvironment.class);
        Messager messager = mock(Messager.class);
        Filer filer = mock(Filer.class);
        Elements elementUtils = mock(Elements.class);
        Types typeUtils = mock(Types.class);
        JavaFileObject javaFileObject = mock(JavaFileObject.class);
        StringWriter stringWriter = new StringWriter();

        lenient().when(processingEnv.getMessager()).thenReturn(messager);
        lenient().when(processingEnv.getFiler()).thenReturn(filer);
        lenient().when(processingEnv.getElementUtils()).thenReturn(elementUtils);
        lenient().when(processingEnv.getTypeUtils()).thenReturn(typeUtils);
        lenient().when(filer.createSourceFile(anyString())).thenReturn(javaFileObject);
        lenient().when(javaFileObject.openWriter()).thenReturn(stringWriter);

        // Stub erasure to return the argument unchanged (identity), since these mocked
        // TypeMirrors already have toString() stubbed with their fully-qualified name.
        lenient().when(typeUtils.erasure(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TypeElement typeElement = createContainerTypeElement(field);
        FilterMetaModelGenerator generator = new FilterMetaModelGenerator(processingEnv);
        generator.generate(typeElement);
        return stringWriter.toString();
    }

    private TypeElement createContainerTypeElement(VariableElement field) {
        TypeElement typeElement = mock(TypeElement.class);
        Name qualifiedName = mock(Name.class);
        Name simpleName = mock(Name.class);
        lenient().when(qualifiedName.toString()).thenReturn(CONTAINER_QUALIFIED);
        lenient().when(simpleName.toString()).thenReturn("TestEntity");
        lenient().when(typeElement.getQualifiedName()).thenReturn(qualifiedName);
        lenient().when(typeElement.getSimpleName()).thenReturn(simpleName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));
        TypeMirror objectType = mock(TypeMirror.class);
        lenient().when(objectType.getKind()).thenReturn(TypeKind.NONE);
        lenient().when(typeElement.getSuperclass()).thenReturn(objectType);
        return typeElement;
    }

    // ==================== Mock Factory Methods ====================

    private DeclaredType createUserType(String qualifiedName, String simpleName, boolean isMetaModel) {
        DeclaredType type = mock(DeclaredType.class);
        TypeElement element = mock(TypeElement.class);
        Name qualName = mock(Name.class);
        Name simpleNameMock = mock(Name.class);

        lenient().when(qualName.toString()).thenReturn(qualifiedName);
        lenient().when(simpleNameMock.toString()).thenReturn(simpleName);
        lenient().when(element.getQualifiedName()).thenReturn(qualName);
        lenient().when(element.getSimpleName()).thenReturn(simpleNameMock);
        lenient().when(element.getKind()).thenReturn(ElementKind.CLASS);
        if (isMetaModel) {
            lenient().when(element.getAnnotation(MetaModel.class)).thenReturn(mock(MetaModel.class));
        }

        lenient().when(type.asElement()).thenReturn(element);
        lenient().when(type.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(type.toString()).thenReturn(qualifiedName);
        lenient().when(type.getTypeArguments()).thenReturn(Collections.emptyList());

        return type;
    }

    private DeclaredType buildListDeclaredType(TypeMirror elementType) {
        DeclaredType listType = mock(DeclaredType.class);
        TypeElement listElement = mock(TypeElement.class);
        Name listQualName = mock(Name.class);
        Name listSimpleName = mock(Name.class);

        lenient().when(listQualName.toString()).thenReturn("java.util.List");
        lenient().when(listSimpleName.toString()).thenReturn("List");
        lenient().when(listElement.getQualifiedName()).thenReturn(listQualName);
        lenient().when(listElement.getSimpleName()).thenReturn(listSimpleName);
        lenient().when(listElement.getKind()).thenReturn(ElementKind.CLASS);

        lenient().when(listType.asElement()).thenReturn(listElement);
        lenient().when(listType.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(listType.toString()).thenReturn("java.util.List<element>");
        lenient().when(listType.getTypeArguments()).thenReturn((List) List.of(elementType));

        return listType;
    }

    private VariableElement buildDirectModelField(String fieldName, String qualifiedName, String simpleName, boolean isMetaModel) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());

        DeclaredType declaredType = createUserType(qualifiedName, simpleName, isMetaModel);
        lenient().when(field.asType()).thenReturn(declaredType);
        return field;
    }

    private VariableElement buildEnumField(String fieldName, String qualifiedName, String simpleName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());

        DeclaredType declaredType = mock(DeclaredType.class);
        TypeElement typeElement = mock(TypeElement.class);
        Name qualName = mock(Name.class);
        Name simpleNameMock = mock(Name.class);
        lenient().when(qualName.toString()).thenReturn(qualifiedName);
        lenient().when(simpleNameMock.toString()).thenReturn(simpleName);
        lenient().when(typeElement.getQualifiedName()).thenReturn(qualName);
        lenient().when(typeElement.getSimpleName()).thenReturn(simpleNameMock);
        lenient().when(typeElement.getKind()).thenReturn(ElementKind.ENUM);

        lenient().when(declaredType.asElement()).thenReturn(typeElement);
        lenient().when(declaredType.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(declaredType.toString()).thenReturn(qualifiedName);
        lenient().when(declaredType.getTypeArguments()).thenReturn(Collections.emptyList());

        lenient().when(field.asType()).thenReturn(declaredType);
        return field;
    }

    private VariableElement buildStandardTypeField(String fieldName, String qualifiedName, String simpleName) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());

        DeclaredType declaredType = mock(DeclaredType.class);
        TypeElement typeElement = mock(TypeElement.class);
        Name qualName = mock(Name.class);
        Name simpleNameMock = mock(Name.class);
        lenient().when(qualName.toString()).thenReturn(qualifiedName);
        lenient().when(simpleNameMock.toString()).thenReturn(simpleName);
        lenient().when(typeElement.getQualifiedName()).thenReturn(qualName);
        lenient().when(typeElement.getSimpleName()).thenReturn(simpleNameMock);
        lenient().when(typeElement.getKind()).thenReturn(ElementKind.CLASS);

        lenient().when(declaredType.asElement()).thenReturn(typeElement);
        lenient().when(declaredType.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(declaredType.toString()).thenReturn(qualifiedName);
        lenient().when(declaredType.getTypeArguments()).thenReturn(Collections.emptyList());

        lenient().when(field.asType()).thenReturn(declaredType);
        return field;
    }

    private VariableElement buildCollectionField(String fieldName, String collectionQualified,
                                                 String elementQualified, String elementSimple, boolean elementIsMetaModel) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());

        DeclaredType elementType = createUserType(elementQualified, elementSimple, elementIsMetaModel);
        DeclaredType collectionType = buildListDeclaredTypeWithQualName(collectionQualified, elementType);
        lenient().when(field.asType()).thenReturn(collectionType);
        return field;
    }

    private VariableElement buildCollectionFieldWithElementType(String fieldName, String collectionQualified, TypeMirror elementType) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());

        DeclaredType collectionType = buildListDeclaredTypeWithQualName(collectionQualified, elementType);
        lenient().when(field.asType()).thenReturn(collectionType);
        return field;
    }

    private DeclaredType buildListDeclaredTypeWithQualName(String qualifiedName, TypeMirror elementType) {
        DeclaredType listType = mock(DeclaredType.class);
        TypeElement listElement = mock(TypeElement.class);
        Name listQualName = mock(Name.class);
        Name listSimpleName = mock(Name.class);

        String simpleName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
        lenient().when(listQualName.toString()).thenReturn(qualifiedName);
        lenient().when(listSimpleName.toString()).thenReturn(simpleName);
        lenient().when(listElement.getQualifiedName()).thenReturn(listQualName);
        lenient().when(listElement.getSimpleName()).thenReturn(listSimpleName);
        lenient().when(listElement.getKind()).thenReturn(ElementKind.CLASS);

        lenient().when(listType.asElement()).thenReturn(listElement);
        lenient().when(listType.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(listType.toString()).thenReturn(qualifiedName + "<element>");
        lenient().when(listType.getTypeArguments()).thenReturn((List) List.of(elementType));

        return listType;
    }
}
