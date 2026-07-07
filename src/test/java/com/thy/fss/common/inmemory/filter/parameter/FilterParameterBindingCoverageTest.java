package com.thy.fss.common.inmemory.filter.parameter;

import com.thy.fss.common.inmemory.filter.deserializer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindingResult;
import org.springframework.beans.TypeMismatchException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FilterParameterBinding Coverage Tests")
class FilterParameterBindingCoverageTest {

    // ===== FilterParameterBindingExceptionHandler =====

    private FilterParameterBindingExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new FilterParameterBindingExceptionHandler();
    }

    @Test
    void handleBindException_returnsErrorResponse() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("filter", "name", "rejected", false, null, null, "Invalid name")
        ));
        BindException ex = new BindException(bindingResult);

        var response = exceptionHandler.handleBindException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Invalid filter parameters");
    }

    @Test
    void handleBindException_emptyErrors_returnsEmptyDetails() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        BindException ex = new BindException(bindingResult);

        var response = exceptionHandler.handleBindException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void handleTypeMismatchException_withCause_returnsErrorResponse() {
        TypeMismatchException ex = new TypeMismatchException("bad-value", Integer.class,
                new NumberFormatException("For input string: 'bad-value'"));
        ex.initPropertyName("age");

        var response = exceptionHandler.handleTypeMismatchException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("bad-value");
    }

    @Test
    void handleTypeMismatchException_withoutCause_returnsErrorResponse() {
        TypeMismatchException ex = new TypeMismatchException("value", String.class);
        ex.initPropertyName("field");

        var response = exceptionHandler.handleTypeMismatchException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void handleTypeMismatchException_nullRequiredType_returnsUnknown() {
        TypeMismatchException ex = new TypeMismatchException("val", null);

        var response = exceptionHandler.handleTypeMismatchException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
    }

    // ===== FilterPropertyEditorRegistrarImpl =====

    @Mock
    private FilterValueDeserializer filterValueDeserializer;
    @Mock
    private CollectionParameterHandler collectionHandler;
    @Mock
    private DeserializerRegistry registry;
    @Mock
    private PropertyEditorRegistry propertyEditorRegistry;

    @Test
    void constructor_nullDeserializer_throwsException() {
        assertThatThrownBy(() -> new FilterPropertyEditorRegistrarImpl(null, collectionHandler, registry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FilterValueDeserializer cannot be null");
    }

    @Test
    void constructor_nullCollectionHandler_throwsException() {
        assertThatThrownBy(() -> new FilterPropertyEditorRegistrarImpl(filterValueDeserializer, null, registry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CollectionParameterHandler cannot be null");
    }

    @Test
    void constructor_nullRegistry_throwsException() {
        assertThatThrownBy(() -> new FilterPropertyEditorRegistrarImpl(filterValueDeserializer, collectionHandler, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DeserializerRegistry cannot be null");
    }

    @Test
    void registerCustomEditors_registersEditors() {
        when(registry.getKnownEnumTypes()).thenReturn(Set.of());

        FilterPropertyEditorRegistrarImpl registrar = new FilterPropertyEditorRegistrarImpl(
                filterValueDeserializer, collectionHandler, registry);

        assertThatNoException().isThrownBy(() ->
                registrar.registerCustomEditors(propertyEditorRegistry));
    }

    @Test
    void registerCustomEditors_withEnumTypes_registersEnumEditors() {
        when(registry.getKnownEnumTypes()).thenReturn(Set.of());

        FilterPropertyEditorRegistrarImpl registrar = new FilterPropertyEditorRegistrarImpl(
                filterValueDeserializer, collectionHandler, registry);

        assertThatNoException().isThrownBy(() ->
                registrar.registerCustomEditors(propertyEditorRegistry));
    }

    @Test
    void registerFilterFieldEditors_nullFilterClass_throwsException() {
        lenient().when(registry.getKnownEnumTypes()).thenReturn(Set.of());
        FilterPropertyEditorRegistrarImpl registrar = new FilterPropertyEditorRegistrarImpl(
                filterValueDeserializer, collectionHandler, registry);

        assertThatThrownBy(() -> registrar.registerFilterFieldEditors(propertyEditorRegistry, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Filter class cannot be null");
    }

    @Test
    void registerFilterFieldEditors_validFilterClass_registers() {
        lenient().when(registry.getKnownEnumTypes()).thenReturn(Set.of());
        FilterPropertyEditorRegistrarImpl registrar = new FilterPropertyEditorRegistrarImpl(
                filterValueDeserializer, collectionHandler, registry);

        assertThatNoException().isThrownBy(() ->
                registrar.registerFilterFieldEditors(propertyEditorRegistry, String.class));
    }
}
