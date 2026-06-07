package com.thy.fss.common.inmemory.filter.autoconfigure;

import com.thy.fss.common.inmemory.filter.parameter.FilterPropertyEditorRegistrar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.web.bind.WebDataBinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for FilterParameterBindingControllerAdvice to achieve 80%+ line coverage.
 */
@DisplayName("FilterParameterBindingControllerAdvice Tests")
class FilterParameterBindingControllerAdviceTest {

    @Test
    @DisplayName("Should create instance with registrar")
    void shouldCreateInstanceWithRegistrar() {
        FilterPropertyEditorRegistrar registrar = mock(FilterPropertyEditorRegistrar.class);
        FilterParameterBindingControllerAdvice advice = new FilterParameterBindingControllerAdvice(registrar);
        assertThat(advice).isNotNull();
    }

    @Test
    @DisplayName("Should call registerCustomEditors on initBinder")
    void shouldCallRegisterCustomEditorsOnInitBinder() {
        FilterPropertyEditorRegistrar registrar = mock(FilterPropertyEditorRegistrar.class);
        FilterParameterBindingControllerAdvice advice = new FilterParameterBindingControllerAdvice(registrar);

        WebDataBinder binder = new WebDataBinder(null);
        advice.initBinder(binder);

        verify(registrar, times(1)).registerCustomEditors(binder);
    }

    @Test
    @DisplayName("Should pass the binder to registrar")
    void shouldPassTheBinderToRegistrar() {
        FilterPropertyEditorRegistrar registrar = mock(FilterPropertyEditorRegistrar.class);
        FilterParameterBindingControllerAdvice advice = new FilterParameterBindingControllerAdvice(registrar);

        Object target = new Object();
        WebDataBinder binder = new WebDataBinder(target);
        advice.initBinder(binder);

        verify(registrar).registerCustomEditors(binder);
    }

    @Test
    @DisplayName("Should work with different binder targets")
    void shouldWorkWithDifferentBinderTargets() {
        FilterPropertyEditorRegistrar registrar = mock(FilterPropertyEditorRegistrar.class);
        FilterParameterBindingControllerAdvice advice = new FilterParameterBindingControllerAdvice(registrar);

        // Test with null target
        WebDataBinder binder1 = new WebDataBinder(null);
        advice.initBinder(binder1);

        // Test with string target
        WebDataBinder binder2 = new WebDataBinder("test");
        advice.initBinder(binder2);

        // Test with object name
        WebDataBinder binder3 = new WebDataBinder(new Object(), "myObject");
        advice.initBinder(binder3);

        verify(registrar, times(3)).registerCustomEditors(any(PropertyEditorRegistry.class));
    }
}
