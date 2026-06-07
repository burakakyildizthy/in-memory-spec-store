package com.thy.fss.common.inmemory.filter.autoconfigure;

import com.thy.fss.common.inmemory.filter.parameter.FilterPropertyEditorRegistrar;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Controller advice that automatically configures WebDataBinder for all controllers
 * to support filter parameter binding.
 *
 * <p>This advice ensures that:
 * <ul>
 *   <li>Nested paths are automatically created (e.g., filter.name.eq=john)</li>
 *   <li>Custom PropertyEditors are registered for filter field types</li>
 * </ul>
 *
 * <p>This is automatically enabled by {@link FilterParameterBindingAutoConfiguration}.
 */
@ControllerAdvice
public class FilterParameterBindingControllerAdvice {

    private final FilterPropertyEditorRegistrar propertyEditorRegistrar;

    public FilterParameterBindingControllerAdvice(FilterPropertyEditorRegistrar propertyEditorRegistrar) {
        this.propertyEditorRegistrar = propertyEditorRegistrar;
    }

    /**
     * Initializes the WebDataBinder for all controller methods.
     * Enables auto-growing of nested paths and registers custom PropertyEditors.
     *
     * @param binder The WebDataBinder to initialize
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Register custom PropertyEditors for filter field types
        propertyEditorRegistrar.registerCustomEditors(binder);

        // Note: setAutoGrowNestedPaths is enabled by default in WebDataBinder
        // and must not be called after initialization (Spring 6.2.9+)
        
        // IMPORTANT: Spring's nested property binding requires editors to be registered
        // for the actual field types, not just the general types. Since we can't predict
        // all possible nested paths at registration time, the PropertyEditors must handle
        // the conversion correctly when Spring calls them.
        //
        // The current implementation registers editors for base types (String, Integer, etc.)
        // which Spring will use when binding to filter fields like StringFilter.eq (String type).
    }
}
