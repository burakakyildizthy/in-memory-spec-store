package com.thy.fss.common.inmemory.filter.parameter;

import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistryImpl;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializerImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.DataBinder;

import java.beans.PropertyEditor;

class PropertyEditorDebugTest {

    @Test
    void debugPropertyEditorRegistration() {
        FilterValueDeserializer valueDeserializer = new FilterValueDeserializerImpl();
        CollectionParameterHandler collectionHandler = new CollectionParameterHandlerImpl(valueDeserializer);
        DeserializerRegistry registry = new DeserializerRegistryImpl();

        StringFilter filter = new StringFilter();
        DataBinder binder = new DataBinder(filter);
        binder.setAutoGrowNestedPaths(true);

        // Create a custom editor that logs calls
        FilterPropertyEditor stringEditor = new FilterPropertyEditor(
                valueDeserializer, collectionHandler, registry, String.class) {
            @Override
            public void setAsText(String text) {
                System.out.println("FilterPropertyEditor.setAsText() called with: " + text);
                super.setAsText(text);
            }

            @Override
            public Object getValue() {
                Object value = super.getValue();
                System.out.println("FilterPropertyEditor.getValue() called, returning: " + value);
                return value;
            }
        };
        binder.registerCustomEditor(String.class, stringEditor);

        System.out.println("=== Before binding ===");
        System.out.println("Filter: " + filter);

        // Try to find editor for "eq" field
        PropertyEditor editorForEq = binder.findCustomEditor(String.class, "eq");
        System.out.println("Editor for 'eq' field: " + editorForEq);
        System.out.println("Editor is our custom editor: " + (editorForEq == stringEditor));

        // Try binding - use actual field name "equals", not JSON property name "eq"
        MutablePropertyValues pvs = new MutablePropertyValues();
        pvs.add("equals", "john");

        System.out.println("\n=== Binding ===");
        binder.bind(pvs);

        System.out.println("\n=== After binding ===");
        System.out.println("Filter: " + filter);
        System.out.println("Filter.equals: " + filter.getEquals());
        System.out.println("Binding errors: " + binder.getBindingResult().getAllErrors());

        Assertions.assertNotNull(filter);
    }
}
