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

import org.assertj.core.api.SoftAssertions;
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
 * Bug Condition Exploration Property Test.
 *
 * **Validates: Requirements 1.2, 1.4**
 *
 * Property 1: Bug Condition - Complete Import Collection Across Nested Type Arguments
 *
 * This test asserts the universal property:
 *   "For every user type referenced by simple name in a generated class,
 *    there MUST be a matching import statement resolved by fully qualified name."
 *
 * The test exercises a bug-triggering field shape by mocking the javax.lang.model
 * type system to simulate a field where a referenced type (Order) lives in a
 * DIFFERENT package than the generated class, nested two collection levels deep.
 *
 * Note: Map-typed field shapes are intentionally NOT exercised here. Map is not a
 * supported/covered field type for this generator, so Map-specific bug cases and
 * their tests have been removed.
 *
 * EXPECTED OUTCOME ON UNFIXED CODE: This test FAILS.
 * The failure confirms the bug exists (missing imports for nested type arguments).
 *
 * After the fix, this test will PASS, confirming the bug is resolved.
 */
class BugConditionExplorationPropertyTest {

    // Fully qualified names for referenced types in a DIFFERENT package
    private static final String ORDER_QUALIFIED = "com.other.pkg.Order";

    // The container class lives in a different package
    private static final String CONTAINER_QUALIFIED = "com.test.BugTriggeringEntity";

    // Expected import statements
    private static final String ORDER_IMPORT = "import com.other.pkg.Order;";

    // ==================== Bug Case 3: List<List<Order>> ====================

    /**
     * Bug Case 3 (Static): List<List<Order>> - doubly-nested collection.
     *
     * The outer first argument List<Order> is a java.util type (excluded by guard),
     * so the nested Order is never reached.
     *
     * Validates: Requirements 1.2, 1.4
     */
    @Property
    @Label("Bug Case 3 Static: List<List<Order>> must import Order in static meta model")
    void doublyNestedCollectionMustBeImportedInStaticMetaModel(
            @ForAll("bugCase3Trigger") String fieldShape) throws Exception {
        String generatedCode = generateStaticMetaModel(createListListOrderField());

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(generatedCode)
                .as("Generated BugTriggeringEntity_ must import Order for field shape: %s. "
                        + "Outer List<Order> is java.util, excluded; nested Order never reached.", fieldShape)
                .contains(ORDER_IMPORT);
        softly.assertAll();
    }

    @Provide
    Arbitrary<String> bugCase3Trigger() {
        return Arbitraries.of("List<List<Order>>");
    }

    // ==================== Universal Property ====================

    /**
     * Universal Property: every user type referenced by simple name must have a matching import.
     *
     * Validates: Requirements 1.2, 1.4
     */
    @Property
    @Label("Universal: every user type referenced by simple name must have a matching import")
    void everyReferencedUserTypeMustHaveImport(
            @ForAll("allExpectedImports") ExpectedImport expectedImport) throws Exception {

        String generatedCode;
        if (expectedImport.isStaticMetaModel) {
            generatedCode = generateStaticMetaModel(expectedImport.createField());
        } else {
            generatedCode = generateFilterMetaModel(expectedImport.createField());
        }

        String className = expectedImport.isStaticMetaModel
                ? "BugTriggeringEntity_" : "BugTriggeringEntityFilter";

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(generatedCode)
                .as("Generated %s must contain '%s' for field shape '%s'. Cause: %s",
                        className, expectedImport.importStatement,
                        expectedImport.fieldShape, expectedImport.cause)
                .contains(expectedImport.importStatement);
        softly.assertAll();
    }

    @Provide
    Arbitrary<ExpectedImport> allExpectedImports() {
        return Arbitraries.of(
                new ExpectedImport("List<List<Order>>", ORDER_IMPORT, true,
                        "Outer first arg List<Order> is java.util, nested Order never reached",
                        FieldShapeFactory.LIST_LIST_ORDER)
        );
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

        // Mock Collection type element for isAssignableToCollection check
        TypeElement collectionTypeElement = mock(TypeElement.class);
        DeclaredType collectionDeclaredType = mock(DeclaredType.class);
        lenient().when(elementUtils.getTypeElement("java.util.Collection"))
                .thenReturn(collectionTypeElement);
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

    // ==================== Type Element / Field Factories ====================

    private TypeElement createContainerTypeElement(VariableElement field) {
        TypeElement typeElement = mock(TypeElement.class);
        Name qualifiedName = mock(Name.class);
        Name simpleName = mock(Name.class);

        lenient().when(qualifiedName.toString()).thenReturn(CONTAINER_QUALIFIED);
        lenient().when(simpleName.toString()).thenReturn("BugTriggeringEntity");
        lenient().when(typeElement.getQualifiedName()).thenReturn(qualifiedName);
        lenient().when(typeElement.getSimpleName()).thenReturn(simpleName);
        lenient().when(typeElement.getEnclosedElements())
                .thenReturn((List) List.of(field));

        // No superclass
        TypeMirror objectType = mock(TypeMirror.class);
        lenient().when(objectType.getKind()).thenReturn(TypeKind.NONE);
        lenient().when(typeElement.getSuperclass()).thenReturn(objectType);

        return typeElement;
    }

    /**
     * Creates a field: List<List<Order>>
     */
    private VariableElement createListListOrderField() {
        return FieldShapeFactory.LIST_LIST_ORDER.create();
    }

    // ==================== Field Shape Factory Enum ====================

    enum FieldShapeFactory {
        LIST_LIST_ORDER {
            @Override
            public VariableElement create() {
                // Inner: List<Order>
                DeclaredType innerList = createListDeclaredType(
                        "java.util.List", createOrderDeclaredType());
                // Outer: List<List<Order>>
                return createCollectionField("listOfListOrder",
                        "java.util.List", innerList);
            }
        };

        public abstract VariableElement create();

        // --- Shared mock factories ---

        static DeclaredType createOrderDeclaredType() {
            DeclaredType orderType = mock(DeclaredType.class);
            TypeElement orderElement = mock(TypeElement.class);
            Name orderQualName = mock(Name.class);
            Name orderSimpleName = mock(Name.class);

            lenient().when(orderQualName.toString()).thenReturn(ORDER_QUALIFIED);
            lenient().when(orderSimpleName.toString()).thenReturn("Order");
            lenient().when(orderElement.getQualifiedName()).thenReturn(orderQualName);
            lenient().when(orderElement.getSimpleName()).thenReturn(orderSimpleName);
            lenient().when(orderElement.getKind()).thenReturn(ElementKind.CLASS);
            lenient().when(orderElement.getAnnotation(MetaModel.class))
                    .thenReturn(mock(MetaModel.class));

            lenient().when(orderType.asElement()).thenReturn(orderElement);
            lenient().when(orderType.getKind()).thenReturn(TypeKind.DECLARED);
            lenient().when(orderType.toString()).thenReturn(ORDER_QUALIFIED);
            lenient().when(orderType.getTypeArguments()).thenReturn(Collections.emptyList());

            return orderType;
        }

        static DeclaredType createListDeclaredType(String listQualifiedName,
                                                    TypeMirror elementType) {
            // Compute toString value BEFORE creating mocks to avoid nested stubbing
            String listToString = listQualifiedName + "<element>";

            DeclaredType listType = mock(DeclaredType.class);
            TypeElement listElement = mock(TypeElement.class);
            Name listQualName = mock(Name.class);
            Name listSimpleName = mock(Name.class);

            lenient().when(listQualName.toString()).thenReturn(listQualifiedName);
            lenient().when(listSimpleName.toString()).thenReturn("List");
            lenient().when(listElement.getQualifiedName()).thenReturn(listQualName);
            lenient().when(listElement.getSimpleName()).thenReturn(listSimpleName);
            lenient().when(listElement.getKind()).thenReturn(ElementKind.CLASS);

            lenient().when(listType.asElement()).thenReturn(listElement);
            lenient().when(listType.getKind()).thenReturn(TypeKind.DECLARED);
            lenient().when(listType.toString()).thenReturn(listToString);
            lenient().when(listType.getTypeArguments())
                    .thenReturn((List) List.of(elementType));

            return listType;
        }

        /**
         * Creates a collection (List/Set) field with the given element type.
         */
        static VariableElement createCollectionField(String fieldName,
                                                     String collectionQualifiedName,
                                                     TypeMirror elementType) {
            VariableElement field = mock(VariableElement.class);
            Name name = mock(Name.class);

            DeclaredType listType = createListDeclaredType(collectionQualifiedName, elementType);

            lenient().when(name.toString()).thenReturn(fieldName);
            lenient().when(field.getSimpleName()).thenReturn(name);
            lenient().when(field.getKind()).thenReturn(ElementKind.FIELD);
            lenient().when(field.getModifiers()).thenReturn(Set.of());
            lenient().when(field.asType()).thenReturn(listType);

            return field;
        }
    }

    // ==================== ExpectedImport Record ====================

    /**
     * Record representing an expected import that should be present in a generated source file.
     */
    record ExpectedImport(String fieldShape, String importStatement,
                          boolean isStaticMetaModel, String cause,
                          FieldShapeFactory factory) {

        public VariableElement createField() {
            return factory.create();
        }

        @Override
        public String toString() {
            String target = isStaticMetaModel ? "BugTriggeringEntity_" : "BugTriggeringEntityFilter";
            return target + " | " + fieldShape + " -> " + importStatement;
        }
    }
}
