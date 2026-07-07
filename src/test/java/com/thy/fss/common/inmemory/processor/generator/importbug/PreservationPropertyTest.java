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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.thy.fss.common.inmemory.processor.MetaModel;
import com.thy.fss.common.inmemory.processor.generator.FilterMetaModelGenerator;
import com.thy.fss.common.inmemory.processor.generator.StaticMetaModelGenerator;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Preservation Property Test.
 *
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
 *
 * Property 2: Preservation - Existing Import Sets Unchanged For Already-Correct Fields
 *
 * This test observes and records the import sets the UNFIXED generators produce for non-buggy
 * fields, then asserts those exact sets remain unchanged after the fix.
 *
 * Observation methodology:
 * - Run the generators on the UNFIXED code
 * - Record what imports are emitted for each field shape
 * - Write assertions that lock in this baseline behavior
 *
 * EXPECTED OUTCOME ON UNFIXED CODE: These tests PASS.
 * After the fix, they must continue to pass (confirming no regressions).
 */
class PreservationPropertyTest {

    private static final String CONTAINER_QUALIFIED = "com.test.PreservationEntity";
    private static final String ORDER_QUALIFIED = "com.other.pkg.Order";
    private static final String STATUS_QUALIFIED = "com.other.pkg.Status";

    // ==================== Requirement 3.2: Direct nested object/enum preservation ====================

    /**
     * Preservation: A direct cross-package @MetaModel field (Order) emits its import in static meta model.
     * Observed on UNFIXED code: the complex-type branch adds "com.other.pkg.Order" to imports.
     *
     * Validates: Requirement 3.2
     */
    @Property
    @Label("Preservation 3.2 Static: Direct @MetaModel Order field emits import")
    void directMetaModelPreservesImportInStaticMetaModel(
            @ForAll("directMetaModelShapes") String shape) throws Exception {
        VariableElement field = buildDirectModelField("order", ORDER_QUALIFIED, "Order", true, false);
        String generatedCode = generateStaticMetaModel(field);

        assertThat(generatedCode)
                .as("Static meta model must import direct @MetaModel Order field")
                .contains("import com.other.pkg.Order;");
    }

    /**
     * Preservation: A direct cross-package enum field (Status) emits its import in static meta model.
     * Observed on UNFIXED code: the complex-type branch adds "com.other.pkg.Status" to imports.
     *
     * Validates: Requirement 3.2
     */
    @Property
    @Label("Preservation 3.2 Static: Direct enum Status field emits import")
    void directEnumPreservesImportInStaticMetaModel(
            @ForAll("directEnumShapes") String shape) throws Exception {
        VariableElement field = buildDirectModelField("status", STATUS_QUALIFIED, "Status", false, true);
        String generatedCode = generateStaticMetaModel(field);

        assertThat(generatedCode)
                .as("Static meta model must import direct enum Status field")
                .contains("import com.other.pkg.Status;");
    }

    /**
     * Preservation: Direct @MetaModel field emits derived filter import in filter class.
     * Observed on UNFIXED code: addFieldImports adds the derived filter class import (OrderFilter).
     * Note: The entity import (Order) depends on how the FilterMetaModelGenerator's
     * internal field analysis works with the mock setup. The key preservation behavior
     * is that the derived filter import IS emitted.
     *
     * Validates: Requirement 3.2
     */
    @Property
    @Label("Preservation 3.2 Filter: Direct @MetaModel Order field emits OrderFilter import")
    void directMetaModelPreservesImportInFilterMetaModel(
            @ForAll("directMetaModelShapes") String shape) throws Exception {
        VariableElement field = buildDirectModelField("order", ORDER_QUALIFIED, "Order", true, false);
        String generatedCode = generateFilterMetaModel(field);

        assertThat(generatedCode)
                .as("Filter must import derived OrderFilter for @MetaModel entity")
                .contains("import com.other.pkg.OrderFilter;");
    }

    /**
     * Preservation: Direct enum field emits its import in filter class.
     * Observed on UNFIXED code: addFieldImports adds the enum import.
     *
     * Validates: Requirement 3.2
     */
    @Property
    @Label("Preservation 3.2 Filter: Direct enum Status field emits import")
    void directEnumPreservesImportInFilterMetaModel(
            @ForAll("directEnumShapes") String shape) throws Exception {
        VariableElement field = buildDirectModelField("status", STATUS_QUALIFIED, "Status", false, true);
        String generatedCode = generateFilterMetaModel(field);

        assertThat(generatedCode)
                .as("Filter must import direct enum Status")
                .contains("import com.other.pkg.Status;");
    }

    @Provide
    Arbitrary<String> directMetaModelShapes() {
        return Arbitraries.of("Order (direct @MetaModel)");
    }

    @Provide
    Arbitrary<String> directEnumShapes() {
        return Arbitraries.of("Status (direct enum)");
    }

    // ==================== Requirement 3.3, 3.4: Standard-type omission preservation ====================

    /**
     * Preservation: Primitives, wrappers, String produce no user-type imports.
     * Observed on UNFIXED code: PRIMITIVE_TYPE_MAPPING check filters them out.
     *
     * Validates: Requirements 3.3, 3.4
     */
    @Property
    @Label("Preservation 3.3/3.4 Static: Standard primitive/wrapper types produce no user import")
    void standardTypesProduceNoUserImportInStaticMetaModel(
            @ForAll("standardPrimitiveShapes") StandardTypeShape shape) throws Exception {
        VariableElement field = buildStandardTypeField(shape.fieldName, shape.qualifiedName, shape.simpleName);
        String generatedCode = generateStaticMetaModel(field);

        assertThat(generatedCode)
                .as("Standard type %s must not produce com.other.pkg import", shape.simpleName)
                .doesNotContain("import com.other.pkg");
        assertThat(generatedCode)
                .as("Must always have base attribute wildcard import")
                .contains("import com.thy.fss.common.inmemory.specification.attribute.*;");
    }

    /**
     * Preservation: Temporal types emit their java.time import but no user-type import.
     * Observed on UNFIXED code: temporals are added via toString().contains() checks.
     *
     * Validates: Requirements 3.3, 3.4
     */
    @Property
    @Label("Preservation 3.3/3.4 Static: Temporal types emit java.time import only")
    void temporalTypesEmitJavaTimeImportOnly(
            @ForAll("temporalTypeShapes") StandardTypeShape shape) throws Exception {
        VariableElement field = buildStandardTypeField(shape.fieldName, shape.qualifiedName, shape.simpleName);
        String generatedCode = generateStaticMetaModel(field);

        assertThat(generatedCode)
                .as("Temporal %s must emit its java.time import", shape.simpleName)
                .contains("import " + shape.qualifiedName + ";");
        assertThat(generatedCode)
                .as("Temporal %s must not emit user-type import", shape.simpleName)
                .doesNotContain("import com.other.pkg");
    }

    @Provide
    Arbitrary<StandardTypeShape> standardPrimitiveShapes() {
        return Arbitraries.of(
                new StandardTypeShape("name", "java.lang.String", "String"),
                new StandardTypeShape("count", "java.lang.Integer", "Integer"),
                new StandardTypeShape("amount", "java.lang.Long", "Long"),
                new StandardTypeShape("rate", "java.lang.Double", "Double"),
                new StandardTypeShape("active", "java.lang.Boolean", "Boolean")
        );
    }

    @Provide
    Arbitrary<StandardTypeShape> temporalTypeShapes() {
        return Arbitraries.of(
                new StandardTypeShape("createdDate", "java.time.LocalDate", "LocalDate"),
                new StandardTypeShape("updatedAt", "java.time.LocalDateTime", "LocalDateTime"),
                new StandardTypeShape("timestamp", "java.time.Instant", "Instant")
        );
    }

    record StandardTypeShape(String fieldName, String qualifiedName, String simpleName) {
        @Override
        public String toString() {
            return simpleName + " (" + qualifiedName + ")";
        }
    }

    // ==================== Requirement 3.5: Base-import preservation ====================

    /**
     * Preservation: Static meta model always has base attribute wildcard import.
     * Observed on UNFIXED code: always added at the start of generateImports.
     *
     * Validates: Requirement 3.5
     */
    @Property
    @Label("Preservation 3.5 Static: Base attribute wildcard import always present")
    void baseImportAlwaysPresentInStaticMetaModel(
            @ForAll("anyNonBuggyFieldShape") String shape) throws Exception {
        VariableElement field = createFieldForShape(shape);
        String generatedCode = generateStaticMetaModel(field);

        assertThat(generatedCode)
                .as("Static meta model must always have attribute wildcard import for shape: %s", shape)
                .contains("import com.thy.fss.common.inmemory.specification.attribute.*;");
    }

    /**
     * Preservation: Filter class always has standard base imports.
     * Observed on UNFIXED code: always added in generateImports.
     *
     * Validates: Requirement 3.5
     */
    @Property
    @Label("Preservation 3.5 Filter: Standard base imports always present")
    void baseImportsAlwaysPresentInFilterMetaModel(
            @ForAll("anyNonBuggyFieldShape") String shape) throws Exception {
        VariableElement field = createFieldForShape(shape);
        String generatedCode = generateFilterMetaModel(field);

        assertThat(generatedCode)
                .as("Filter must have java.util.Objects import")
                .contains("import java.util.Objects;");
        assertThat(generatedCode)
                .as("Filter must have java.util.function.Function import")
                .contains("import java.util.function.Function;");
        assertThat(generatedCode)
                .as("Filter must have java.time.LocalDate import")
                .contains("import java.time.LocalDate;");
        assertThat(generatedCode)
                .as("Filter must have java.time.LocalDateTime import")
                .contains("import java.time.LocalDateTime;");
        assertThat(generatedCode)
                .as("Filter must have java.time.Instant import")
                .contains("import java.time.Instant;");
        assertThat(generatedCode)
                .as("Filter must have Jackson JsonDeserialize import")
                .contains("import com.fasterxml.jackson.databind.annotation.JsonDeserialize;");
        assertThat(generatedCode)
                .as("Filter must have EntityFilter import")
                .contains("import com.thy.fss.common.inmemory.filter.EntityFilter;");
    }

    @Provide
    Arbitrary<String> anyNonBuggyFieldShape() {
        return Arbitraries.of("String", "Integer", "Long", "Double", "Boolean",
                "LocalDate", "LocalDateTime", "Instant", "DirectOrder", "DirectEnum");
    }

    private VariableElement createFieldForShape(String shape) {
        return switch (shape) {
            case "String" -> buildStandardTypeField("name", "java.lang.String", "String");
            case "Integer" -> buildStandardTypeField("count", "java.lang.Integer", "Integer");
            case "Long" -> buildStandardTypeField("amount", "java.lang.Long", "Long");
            case "Double" -> buildStandardTypeField("rate", "java.lang.Double", "Double");
            case "Boolean" -> buildStandardTypeField("active", "java.lang.Boolean", "Boolean");
            case "LocalDate" -> buildStandardTypeField("createdDate", "java.time.LocalDate", "LocalDate");
            case "LocalDateTime" -> buildStandardTypeField("updatedAt", "java.time.LocalDateTime", "LocalDateTime");
            case "Instant" -> buildStandardTypeField("timestamp", "java.time.Instant", "Instant");
            case "DirectOrder" -> buildDirectModelField("order", ORDER_QUALIFIED, "Order", true, false);
            case "DirectEnum" -> buildDirectModelField("status", STATUS_QUALIFIED, "Status", false, true);
            default -> buildStandardTypeField("name", "java.lang.String", "String");
        };
    }

    // ==================== Requirement 3.6: Inheritance preservation ====================

    /**
     * Preservation: Inherited fields from @MetaModel superclass contribute imports.
     * Observed on UNFIXED code: getAllFields recursively collects from @MetaModel parents.
     *
     * Validates: Requirement 3.6
     */
    @Property
    @Label("Preservation 3.6 Static: Inherited @MetaModel superclass fields contribute imports")
    void inheritedFieldsContributeImportsInStaticMetaModel(
            @ForAll("inheritanceShapes") String shape) throws Exception {
        String generatedCode = generateStaticMetaModelWithInheritance();

        assertThat(generatedCode)
                .as("Inherited Order field from @MetaModel superclass must produce import")
                .contains("import com.other.pkg.Order;");
        assertThat(generatedCode)
                .as("Base attribute wildcard import must be present")
                .contains("import com.thy.fss.common.inmemory.specification.attribute.*;");
    }

    @Provide
    Arbitrary<String> inheritanceShapes() {
        return Arbitraries.of("SubclassWithInheritedOrderField");
    }

    // ==================== Requirement 3.1: Single-argument collection preservation ====================

    /**
     * Preservation: List<Order> in static meta model produces java.util.Collection import
     * and generates CollectionAttribute with the correct element type.
     * Observed on UNFIXED code: isCollectionType detects the collection, adds java.util.Collection.
     * Note: The element type import (com.other.pkg.Order) is NOT emitted by the unfixed code
     * for cross-package types in the static meta model because the code only adds the import
     * if the collection element type passes through the collection-element-import path AND
     * the complex-type path. For same-package cases this works implicitly.
     *
     * Validates: Requirement 3.1
     */
    @Property
    @Label("Preservation 3.1 Static: List<Order> produces Collection import and correct attribute")
    void listOrderProducesCollectionImportInStaticMetaModel(
            @ForAll("collectionShapes") String collectionType) throws Exception {
        VariableElement field = buildCollectionField("orders", collectionType, ORDER_QUALIFIED, "Order", true);
        String generatedCode = generateStaticMetaModel(field);

        assertThat(generatedCode)
                .as("Static meta model must add java.util.Collection import for collection field")
                .contains("import java.util.Collection;");
        assertThat(generatedCode)
                .as("Static meta model must generate CollectionAttribute declaration")
                .contains("CollectionAttribute<PreservationEntity, Order>");
    }

    /**
     * Preservation: List<Order> in filter class detects the collection type and uses CollectionFilter.
     * Observed on UNFIXED code: isCollectionType detects it, determineFilterType returns CollectionFilter.
     *
     * Validates: Requirement 3.1
     */
    @Property
    @Label("Preservation 3.1 Filter: List<Order> produces CollectionFilter declaration")
    void listOrderProducesCollectionFilterInFilterMetaModel(
            @ForAll("collectionShapes") String collectionType) throws Exception {
        VariableElement field = buildCollectionField("orders", collectionType, ORDER_QUALIFIED, "Order", true);
        String generatedCode = generateFilterMetaModel(field);

        assertThat(generatedCode)
                .as("Filter must have CollectionFilter import")
                .contains("import com.thy.fss.common.inmemory.filter.CollectionFilter;");
        assertThat(generatedCode)
                .as("Filter must declare CollectionFilter field")
                .contains("CollectionFilter<");
    }

    @Provide
    Arbitrary<String> collectionShapes() {
        return Arbitraries.of("java.util.List", "java.util.Set");
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

        TypeElement typeElement = createContainerTypeElement(field);
        FilterMetaModelGenerator generator = new FilterMetaModelGenerator(processingEnv);
        generator.generate(typeElement);
        return stringWriter.toString();
    }

    private String generateStaticMetaModelWithInheritance() throws Exception {
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

        TypeElement typeElement = createContainerTypeElementWithInheritance(typeUtils);
        StaticMetaModelGenerator generator = new StaticMetaModelGenerator(processingEnv);
        generator.generate(typeElement);
        return stringWriter.toString();
    }

    // ==================== Type Element Factories ====================

    private TypeElement createContainerTypeElement(VariableElement field) {
        TypeElement typeElement = mock(TypeElement.class);
        Name qualifiedName = mock(Name.class);
        Name simpleName = mock(Name.class);
        lenient().when(qualifiedName.toString()).thenReturn(CONTAINER_QUALIFIED);
        lenient().when(simpleName.toString()).thenReturn("PreservationEntity");
        lenient().when(typeElement.getQualifiedName()).thenReturn(qualifiedName);
        lenient().when(typeElement.getSimpleName()).thenReturn(simpleName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));
        TypeMirror objectType = mock(TypeMirror.class);
        lenient().when(objectType.getKind()).thenReturn(TypeKind.NONE);
        lenient().when(typeElement.getSuperclass()).thenReturn(objectType);
        return typeElement;
    }

    private TypeElement createContainerTypeElementWithInheritance(Types typeUtils) {
        // Superclass with a direct Order field
        VariableElement inheritedField = buildDirectModelField("order", ORDER_QUALIFIED, "Order", true, false);
        TypeElement superTypeElement = mock(TypeElement.class);
        Name superQualName = mock(Name.class);
        Name superSimpleName = mock(Name.class);
        lenient().when(superQualName.toString()).thenReturn("com.test.BaseEntity");
        lenient().when(superSimpleName.toString()).thenReturn("BaseEntity");
        lenient().when(superTypeElement.getQualifiedName()).thenReturn(superQualName);
        lenient().when(superTypeElement.getSimpleName()).thenReturn(superSimpleName);
        lenient().when(superTypeElement.getAnnotation(MetaModel.class)).thenReturn(mock(MetaModel.class));
        lenient().when(superTypeElement.getEnclosedElements()).thenReturn((List) List.of(inheritedField));
        TypeMirror objectType = mock(TypeMirror.class);
        lenient().when(objectType.getKind()).thenReturn(TypeKind.NONE);
        lenient().when(superTypeElement.getSuperclass()).thenReturn(objectType);

        // Subclass with a String field
        VariableElement ownField = buildStandardTypeField("name", "java.lang.String", "String");
        TypeElement subTypeElement = mock(TypeElement.class);
        Name subQualName = mock(Name.class);
        Name subSimpleName = mock(Name.class);
        lenient().when(subQualName.toString()).thenReturn("com.test.SubEntity");
        lenient().when(subSimpleName.toString()).thenReturn("SubEntity");
        lenient().when(subTypeElement.getQualifiedName()).thenReturn(subQualName);
        lenient().when(subTypeElement.getSimpleName()).thenReturn(subSimpleName);
        lenient().when(subTypeElement.getEnclosedElements()).thenReturn((List) List.of(ownField));

        DeclaredType superclassType = mock(DeclaredType.class);
        lenient().when(superclassType.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(subTypeElement.getSuperclass()).thenReturn(superclassType);
        lenient().when(typeUtils.asElement(superclassType)).thenReturn(superTypeElement);

        return subTypeElement;
    }

    // ==================== Static Field Builders ====================

    static VariableElement buildDirectModelField(String fieldName, String qualifiedName,
                                                  String simpleName, boolean isMetaModel, boolean isEnum) {
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
        if (isEnum) {
            lenient().when(typeElement.getKind()).thenReturn(ElementKind.ENUM);
        } else {
            lenient().when(typeElement.getKind()).thenReturn(ElementKind.CLASS);
        }
        if (isMetaModel) {
            lenient().when(typeElement.getAnnotation(MetaModel.class)).thenReturn(mock(MetaModel.class));
        }

        lenient().when(declaredType.asElement()).thenReturn(typeElement);
        lenient().when(declaredType.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(declaredType.toString()).thenReturn(qualifiedName);
        lenient().when(declaredType.getTypeArguments()).thenReturn(Collections.emptyList());

        lenient().when(field.asType()).thenReturn(declaredType);
        return field;
    }

    static VariableElement buildStandardTypeField(String fieldName, String qualifiedName, String simpleName) {
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

    static VariableElement buildCollectionField(String fieldName, String collectionQualified,
                                                String elementQualified, String elementSimple,
                                                boolean elementIsMetaModel) {
        VariableElement field = mock(VariableElement.class);
        Name name = mock(Name.class);
        lenient().when(name.toString()).thenReturn(fieldName);
        lenient().when(field.getSimpleName()).thenReturn(name);
        lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
        lenient().when(field.getModifiers()).thenReturn(Set.of());

        // Collection outer type
        DeclaredType collectionType = mock(DeclaredType.class);
        TypeElement collectionElement = mock(TypeElement.class);
        Name collQualName = mock(Name.class);
        Name collSimpleName = mock(Name.class);
        String collSimple = collectionQualified.substring(collectionQualified.lastIndexOf('.') + 1);
        lenient().when(collQualName.toString()).thenReturn(collectionQualified);
        lenient().when(collSimpleName.toString()).thenReturn(collSimple);
        lenient().when(collectionElement.getQualifiedName()).thenReturn(collQualName);
        lenient().when(collectionElement.getSimpleName()).thenReturn(collSimpleName);
        lenient().when(collectionElement.getKind()).thenReturn(ElementKind.CLASS);
        lenient().when(collectionType.asElement()).thenReturn(collectionElement);
        lenient().when(collectionType.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(collectionType.toString()).thenReturn(collectionQualified + "<" + elementQualified + ">");

        // Element type
        DeclaredType elementType = mock(DeclaredType.class);
        TypeElement elementTypeElement = mock(TypeElement.class);
        Name elemQualName = mock(Name.class);
        Name elemSimpleName = mock(Name.class);
        lenient().when(elemQualName.toString()).thenReturn(elementQualified);
        lenient().when(elemSimpleName.toString()).thenReturn(elementSimple);
        lenient().when(elementTypeElement.getQualifiedName()).thenReturn(elemQualName);
        lenient().when(elementTypeElement.getSimpleName()).thenReturn(elemSimpleName);
        lenient().when(elementTypeElement.getKind()).thenReturn(ElementKind.CLASS);
        if (elementIsMetaModel) {
            lenient().when(elementTypeElement.getAnnotation(MetaModel.class)).thenReturn(mock(MetaModel.class));
        }
        lenient().when(elementType.asElement()).thenReturn(elementTypeElement);
        lenient().when(elementType.getKind()).thenReturn(TypeKind.DECLARED);
        lenient().when(elementType.toString()).thenReturn(elementQualified);
        lenient().when(elementType.getTypeArguments()).thenReturn(Collections.emptyList());

        // Wire type arguments
        lenient().when(collectionType.getTypeArguments()).thenReturn((List) List.of(elementType));
        lenient().when(field.asType()).thenReturn(collectionType);
        return field;
    }
}
