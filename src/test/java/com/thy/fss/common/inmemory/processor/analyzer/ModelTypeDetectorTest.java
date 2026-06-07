package com.thy.fss.common.inmemory.processor.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for ModelTypeDetector.
 * <p>
 * Tests model type detection logic, filter class name resolution,
 * and qualified name resolution for both model types and basic types.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModelTypeDetectorTest {

    private static final String USER_FILTER = "UserFilter";
    private static final String COM_EXAMPLE_USER = "com.example.User";

    @Mock
    private ProcessingEnvironment processingEnv;

    @Mock
    private Elements elementUtils;

    @Mock
    private TypeElement mockFilterElement;

    @Mock
    private TypeElement mockTypeElement;

    @BeforeEach
    void setUp() {
        lenient().when(processingEnv.getElementUtils()).thenReturn(elementUtils);
    }

    // ========== Filter Class Name Resolution Tests ==========

    @Test
    void getFilterClassNameWithSimpleNameReturnsFilterClassName() {
        String result = ModelTypeDetector.getFilterClassName("User");

        assertThat(result).isEqualTo(USER_FILTER);
    }

    @Test
    void getFilterClassNameWithQualifiedNameReturnsFilterClassName() {
        String result = ModelTypeDetector.getFilterClassName("com.example.User");

        assertThat(result).isEqualTo(USER_FILTER);
    }

    @Test
    void getFilterClassNameWithMultiWordNameReturnsFilterClassName() {
        String result = ModelTypeDetector.getFilterClassName("OrderItem");

        assertThat(result).isEqualTo("OrderItemFilter");
    }

    @Test
    void getFilterClassNameWithNestedPackageReturnsFilterClassName() {
        String result = ModelTypeDetector.getFilterClassName("com.example.model.Address");

        assertThat(result).isEqualTo("AddressFilter");
    }

    @Test
    void getFilterClassNameWithNullNameReturnsNull() {
        String result = ModelTypeDetector.getFilterClassName(null);

        assertThat(result).isNull();
    }

    @Test
    void getFilterClassNameWithEmptyNameReturnsNull() {
        String result = ModelTypeDetector.getFilterClassName("");

        assertThat(result).isNull();
    }

    // ========== Qualified Filter Class Name Resolution Tests ==========

    @Test
    void getQualifiedFilterClassNameWithQualifiedNameReturnsQualifiedFilterClassName() {
        String result = ModelTypeDetector.getQualifiedFilterClassName("com.example.User", processingEnv);

        assertThat(result).isEqualTo("com.example.UserFilter");
    }

    @Test
    void getQualifiedFilterClassNameWithNestedPackageReturnsQualifiedFilterClassName() {
        String result = ModelTypeDetector.getQualifiedFilterClassName("com.example.model.Address", processingEnv);

        assertThat(result).isEqualTo("com.example.model.AddressFilter");
    }

    @Test
    void getQualifiedFilterClassNameWithSimpleNameReturnsSimpleFilterClassName() {
        String result = ModelTypeDetector.getQualifiedFilterClassName("User", processingEnv);

        assertThat(result).isEqualTo(USER_FILTER);
    }

    @Test
    void getQualifiedFilterClassNameWithNullNameReturnsNull() {
        String result = ModelTypeDetector.getQualifiedFilterClassName(null, processingEnv);

        assertThat(result).isNull();
    }

    @Test
    void getQualifiedFilterClassNameWithEmptyNameReturnsNull() {
        String result = ModelTypeDetector.getQualifiedFilterClassName("", processingEnv);

        assertThat(result).isNull();
    }

    // ========== Model Type Detection Tests ==========

    @Test
    void isModelTypeWithExistingFilterClassReturnsTrue() {
        // Setup: Filter class exists
        String typeName = COM_EXAMPLE_USER;
        String filterClassName = "com.example.UserFilter";
        lenient().when(elementUtils.getTypeElement(typeName)).thenReturn(mockTypeElement);
        lenient().when(mockTypeElement.getKind()).thenReturn(javax.lang.model.element.ElementKind.CLASS);
        lenient().when(elementUtils.getTypeElement(filterClassName)).thenReturn(mockFilterElement);

        boolean result = ModelTypeDetector.isModelType(typeName, processingEnv);

        assertThat(result).isTrue();
    }

    @Test
    void isModelTypeWithNonExistingFilterClassReturnsFalse() {
        // Setup: Filter class does not exist
        String typeName = "com.example.UnknownType";
        String filterClassName = "com.example.UnknownTypeFilter";
        lenient().when(elementUtils.getTypeElement(typeName)).thenReturn(mockTypeElement);
        lenient().when(mockTypeElement.getKind()).thenReturn(javax.lang.model.element.ElementKind.CLASS);
        lenient().when(elementUtils.getTypeElement(filterClassName)).thenReturn(null);

        boolean result = ModelTypeDetector.isModelType(typeName, processingEnv);

        assertThat(result).isFalse();
    }

    @Test
    void isModelTypeWithBasicTypeReturnsFalse() {
        // Setup: Basic types don't have filter classes in their package
        String typeName = "java.lang.String";
        String filterClassName = "java.lang.StringFilter";
        lenient().when(elementUtils.getTypeElement(typeName)).thenReturn(mockTypeElement);
        lenient().when(mockTypeElement.getKind()).thenReturn(javax.lang.model.element.ElementKind.CLASS);
        lenient().when(elementUtils.getTypeElement(filterClassName)).thenReturn(null);

        boolean result = ModelTypeDetector.isModelType(typeName, processingEnv);

        assertThat(result).isFalse();
    }

    @Test
    void isModelTypeWithNullTypeNameReturnsFalse() {
        boolean result = ModelTypeDetector.isModelType(null, processingEnv);

        assertThat(result).isFalse();
    }

    @Test
    void isModelTypeWithEmptyTypeNameReturnsFalse() {
        boolean result = ModelTypeDetector.isModelType("", processingEnv);

        assertThat(result).isFalse();
    }

    @Test
    void isModelTypeWithNestedPackageAndExistingFilterReturnsTrue() {
        // Setup: Filter class exists in nested package
        String typeName = "com.example.model.entity.Customer";
        String filterClassName = "com.example.model.entity.CustomerFilter";
        lenient().when(elementUtils.getTypeElement(typeName)).thenReturn(mockTypeElement);
        lenient().when(mockTypeElement.getKind()).thenReturn(javax.lang.model.element.ElementKind.CLASS);
        lenient().when(elementUtils.getTypeElement(filterClassName)).thenReturn(mockFilterElement);

        boolean result = ModelTypeDetector.isModelType(typeName, processingEnv);

        assertThat(result).isTrue();
    }

    // ========== Edge Case Tests ==========

    @Test
    void getFilterClassNameWithSingleCharacterNameReturnsFilterClassName() {
        String result = ModelTypeDetector.getFilterClassName("A");

        assertThat(result).isEqualTo("AFilter");
    }

    @Test
    void getFilterClassNameWithNameEndingInFilterAppendsFilter() {
        // Even if name ends with "Filter", we still append "Filter"
        // This is expected behavior - UserFilter -> UserFilterFilter
        String result = ModelTypeDetector.getFilterClassName(USER_FILTER);

        assertThat(result).isEqualTo("UserFilterFilter");
    }

    @Test
    void getQualifiedFilterClassNameWithDefaultPackageReturnsSimpleFilterClassName() {
        // Type in default package (no dots)
        String result = ModelTypeDetector.getQualifiedFilterClassName("SimpleType", processingEnv);

        assertThat(result).isEqualTo("SimpleTypeFilter");
    }

    @Test
    void isModelTypeWithMultipleDotsInPackageHandlesCorrectly() {
        // Setup: Type with deep package hierarchy
        String typeName = "com.thy.fss.common.inmemory.testmodel.User";
        String filterClassName = "com.thy.fss.common.inmemory.testmodel.UserFilter";
        lenient().when(elementUtils.getTypeElement(typeName)).thenReturn(mockTypeElement);
        lenient().when(mockTypeElement.getKind()).thenReturn(javax.lang.model.element.ElementKind.CLASS);
        lenient().when(elementUtils.getTypeElement(filterClassName)).thenReturn(mockFilterElement);

        boolean result = ModelTypeDetector.isModelType(typeName, processingEnv);

        assertThat(result).isTrue();
    }
}
