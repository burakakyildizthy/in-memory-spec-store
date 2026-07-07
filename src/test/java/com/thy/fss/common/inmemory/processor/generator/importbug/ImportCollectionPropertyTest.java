package com.thy.fss.common.inmemory.processor.generator.importbug;

import java.io.StringWriter;
import java.util.ArrayList;
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
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based tests for import collection using jqwik.
 *
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
 *
 * Properties tested:
 * 1. Every user type reachable through any type argument appears in collected imports (static meta model)
 * 2. Every user type reachable has a corresponding ...Filter import when type is @MetaModel (filter model)
 * 3. Standard-library types never produce user-type imports
 */
class ImportCollectionPropertyTest {

    private static final String CONTAINER_QUALIFIED = "com.test.GeneratedEntity";

    // User types in various packages to test cross-package resolution
    private static final String[] USER_TYPES = {
            "com.acme.domain.Alpha",
            "com.other.pkg.Beta",
            "org.example.Gamma",
            "com.deep.nested.pkg.Delta"
    };

    // Standard library types that should never be imported as user types
    private static final String[] STANDARD_TYPES = {
            "java.lang.String",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Double",
            "java.lang.Boolean",
            "java.util.List",
            "java.util.Map",
            "java.util.Set",
            "java.util.Collection"
    };

    // ==================== Property 1: User types always imported in static meta model ====================

    /**
     * Property: For any random nested generic type structure, every user type reachable through
     * any type argument at any depth MUST appear in the collected imports of the static meta model.
     *
     * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**
     */
    @Property(tries = 50)
    @Label("Every reachable user type appears in static meta model imports")
    void everyReachableUserTypeAppearsInStaticImports(
            @ForAll("randomNestedTypeStructures") TypeStructure typeStructure) throws Exception {

        VariableElement field = typeStructure.toMockedField("testField");
        String generated = generateStaticMetaModel(field);

        for (String userType : typeStructure.getReachableUserTypes()) {
            assertThat(generated)
                    .as("Static meta model must import user type '%s' from type structure: %s",
                            userType, typeStructure.describe())
                    .contains("import " + userType + ";");
        }
    }

    // ==================== Property 2: Filter imports for @MetaModel references ====================

    /**
     * Property: For any random type structure with @MetaModel references, the filter generator
     * MUST collect both the entity import and the derived ...Filter import.
     *
     * **Validates: Requirements 1.5, 2.1, 2.2, 2.3, 2.4, 2.5**
     */
    @Property(tries = 50)
    @Label("Every reachable @MetaModel type produces a ...Filter import in filter class")
    void everyReachableMetaModelProducesFilterImport(
            @ForAll("randomMetaModelTypeStructures") TypeStructure typeStructure) throws Exception {

        VariableElement field = typeStructure.toMockedField("testField");
        String generated = generateFilterMetaModel(field);

        for (String userType : typeStructure.getReachableMetaModelTypes()) {
            String simpleName = userType.substring(userType.lastIndexOf('.') + 1);
            String filterImport = userType.replace(simpleName, simpleName + "Filter");
            assertThat(generated)
                    .as("Filter must import entity '%s' from structure: %s", userType, typeStructure.describe())
                    .contains("import " + userType + ";");
            assertThat(generated)
                    .as("Filter must import derived '%sFilter' from structure: %s", simpleName, typeStructure.describe())
                    .contains("import " + filterImport + ";");
        }
    }

    // ==================== Property 3: Standard types never produce user imports ====================

    /**
     * Property: For any type structure composed ONLY of standard-library types,
     * no user-type import (com.*, org.*) is produced.
     *
     * **Validates: Requirements 3.3, 3.4**
     */
    @Property(tries = 50)
    @Label("Standard-library-only type structures produce no user-type imports")
    void standardTypeOnlyStructuresProduceNoUserImports(
            @ForAll("standardOnlyTypeStructures") TypeStructure typeStructure) throws Exception {

        VariableElement field = typeStructure.toMockedField("standardField");
        String generated = generateStaticMetaModel(field);

        assertThat(generated)
                .as("Standard-type-only structure must not import com.other.pkg or com.acme types: %s",
                        typeStructure.describe())
                .doesNotContain("import com.other.pkg")
                .doesNotContain("import com.acme")
                .doesNotContain("import org.example");
    }

    // ==================== Providers ====================

    @Provide
    Arbitrary<TypeStructure> randomNestedTypeStructures() {
        // Generate random type trees with at least one user type
        Arbitrary<String> userTypeArb = Arbitraries.of(USER_TYPES);
        Arbitrary<String> anyTypeArb = Arbitraries.of(
                "java.lang.String", "java.lang.Integer",
                USER_TYPES[0], USER_TYPES[1], USER_TYPES[2], USER_TYPES[3]);
        Arbitrary<Integer> depthArb = Arbitraries.integers().between(1, 3);

        return Combinators.combine(userTypeArb, anyTypeArb, depthArb)
                .as((mandatoryUserType, otherType, depth) ->
                        buildRandomStructure(mandatoryUserType, otherType, depth, true));
    }

    @Provide
    Arbitrary<TypeStructure> randomMetaModelTypeStructures() {
        Arbitrary<String> userTypeArb = Arbitraries.of(USER_TYPES);
        Arbitrary<Integer> depthArb = Arbitraries.integers().between(1, 3);

        return Combinators.combine(userTypeArb, depthArb)
                .as((userType, depth) ->
                        buildRandomStructure(userType, "java.lang.String", depth, true));
    }

    @Provide
    Arbitrary<TypeStructure> standardOnlyTypeStructures() {
        Arbitrary<String> stdTypeArb = Arbitraries.of(STANDARD_TYPES);
        Arbitrary<Integer> depthArb = Arbitraries.integers().between(0, 2);

        return Combinators.combine(stdTypeArb, depthArb)
                .as((stdType, depth) -> buildRandomStructure(stdType, "java.lang.Integer", depth, false));
    }

    // ==================== TypeStructure builder ====================

    private TypeStructure buildRandomStructure(String primaryType, String secondaryType, int depth, boolean primaryIsMetaModel) {
        // Note: Map-typed structures are intentionally not exercised here. Map is not a
        // supported/covered field type for this generator, so structures are built using
        // only List nesting.
        if (depth <= 0) {
            return new TypeStructure(primaryType, getSimpleName(primaryType), primaryIsMetaModel, Collections.emptyList());
        }

        // Build nested List structures based on depth
        // depth 1: List<primaryType>
        // depth 2: List<List<primaryType>>
        // depth 3: List<List<List<primaryType>>>

        TypeStructure leafNode = new TypeStructure(primaryType, getSimpleName(primaryType), primaryIsMetaModel, Collections.emptyList());

        TypeStructure current = leafNode;
        for (int i = 0; i < depth; i++) {
            current = new TypeStructure("java.util.List", "List", false, List.of(current));
        }
        return current;
    }

    private String getSimpleName(String qualifiedName) {
        return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    }

    // ==================== TypeStructure record ====================

    /**
     * Represents a type structure for property-based testing.
     * Can be a leaf type or a parameterized type with type arguments.
     */
    record TypeStructure(String qualifiedName, String simpleName, boolean isMetaModel, List<TypeStructure> typeArguments) {

        String describe() {
            if (typeArguments.isEmpty()) {
                return simpleName;
            }
            StringBuilder sb = new StringBuilder(simpleName).append("<");
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(typeArguments.get(i).describe());
            }
            return sb.append(">").toString();
        }

        List<String> getReachableUserTypes() {
            List<String> result = new ArrayList<>();
            collectUserTypes(this, result);
            return result;
        }

        List<String> getReachableMetaModelTypes() {
            List<String> result = new ArrayList<>();
            collectMetaModelTypes(this, result);
            return result;
        }

        private void collectUserTypes(TypeStructure node, List<String> result) {
            if (!node.qualifiedName.startsWith("java.lang") &&
                    !node.qualifiedName.startsWith("java.util") &&
                    !result.contains(node.qualifiedName)) {
                result.add(node.qualifiedName);
            }
            for (TypeStructure child : node.typeArguments) {
                collectUserTypes(child, result);
            }
        }

        private void collectMetaModelTypes(TypeStructure node, List<String> result) {
            if (node.isMetaModel && !result.contains(node.qualifiedName)) {
                result.add(node.qualifiedName);
            }
            for (TypeStructure child : node.typeArguments) {
                collectMetaModelTypes(child, result);
            }
        }

        DeclaredType toMockedType() {
            // Build child type mocks FIRST before setting up this type's stubs
            List<TypeMirror> mockArgs = new ArrayList<>();
            if (!typeArguments.isEmpty()) {
                for (TypeStructure arg : typeArguments) {
                    mockArgs.add(arg.toMockedType());
                }
            }

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

            if (mockArgs.isEmpty()) {
                lenient().when(type.getTypeArguments()).thenReturn(Collections.emptyList());
            } else {
                lenient().when(type.getTypeArguments()).thenReturn((List) mockArgs);
            }

            return type;
        }

        VariableElement toMockedField(String fieldName) {
            // Build the type mock FIRST to avoid nested stub-inside-stub errors
            DeclaredType mockedType = toMockedType();

            VariableElement field = mock(VariableElement.class);
            Name name = mock(Name.class);
            lenient().when(name.toString()).thenReturn(fieldName);
            lenient().when(field.getSimpleName()).thenReturn(name);
            lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
            lenient().when(field.getModifiers()).thenReturn(Set.of());
            lenient().when(field.asType()).thenReturn(mockedType);
            return field;
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
        lenient().when(simpleName.toString()).thenReturn("GeneratedEntity");
        lenient().when(typeElement.getQualifiedName()).thenReturn(qualifiedName);
        lenient().when(typeElement.getSimpleName()).thenReturn(simpleName);
        lenient().when(typeElement.getEnclosedElements()).thenReturn((List) List.of(field));
        TypeMirror objectType = mock(TypeMirror.class);
        lenient().when(objectType.getKind()).thenReturn(TypeKind.NONE);
        lenient().when(typeElement.getSuperclass()).thenReturn(objectType);
        return typeElement;
    }
}
