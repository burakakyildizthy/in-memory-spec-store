package com.thy.fss.common.inmemory.filter.parameter;

import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistryImpl;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializerImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.bind.WebDataBinder;

/**
 * Integration test to verify that the filter parameter binding works correctly
 * with Spring's DataBinder, simulating real Spring MVC behavior.
 */
@DisplayName("Spring DataBinder Integration Test")
class SpringDataBinderIntegrationTest {

    private FilterValueDeserializer valueDeserializer;
    private CollectionParameterHandler collectionHandler;
    private DeserializerRegistry registry;
    private FilterPropertyEditorRegistrar editorRegistrar;

    @BeforeEach
    void setUp() {
        valueDeserializer = new FilterValueDeserializerImpl();
        collectionHandler = new CollectionParameterHandlerImpl(valueDeserializer);
        registry = new DeserializerRegistryImpl();
        editorRegistrar = new FilterPropertyEditorRegistrarImpl(valueDeserializer, collectionHandler, registry);
    }

    @Test
    @DisplayName("Should bind simple StringFilter field with eq operation")
    void shouldBindSimpleStringFilterField() {
        // Given - simulating Spring MVC binding: ?name.eq=john
        UserCriteria criteria = new UserCriteria();
        WebDataBinder binder = new WebDataBinder(criteria);

        // Simulate what FilterParameterBindingControllerAdvice does
        binder.setAutoGrowNestedPaths(true);
        editorRegistrar.registerCustomEditors(binder);

        MutablePropertyValues pvs = new MutablePropertyValues();
        pvs.add("name.equals", "john");

        // When
        binder.bind(pvs);

        // Then - print results for debugging
        System.out.println("=== Test: shouldBindSimpleStringFilterField ===");
        System.out.println("Binding result errors: " + binder.getBindingResult().getAllErrors());
        System.out.println("Binding result field errors: " + binder.getBindingResult().getFieldErrors());
        System.out.println("Name filter: " + criteria.getName());
        if (criteria.getName() != null) {
            System.out.println("Name.equals: " + criteria.getName().getEquals());
            System.out.println("Name.contains: " + criteria.getName().getContains());
        }
        System.out.println();
        Assertions.assertNotNull(criteria.getName());
    }

    @Test
    @DisplayName("Should bind StringFilter with multiple operations")
    void shouldBindMultipleOperations() {
        // Given - simulating: ?name.cont=john&name.start=J
        UserCriteria criteria = new UserCriteria();
        WebDataBinder binder = new WebDataBinder(criteria);
        binder.setAutoGrowNestedPaths(true);
        editorRegistrar.registerCustomEditors(binder);

        MutablePropertyValues pvs = new MutablePropertyValues();
        pvs.add("name.cont", "john");
        pvs.add("name.start", "J");

        // When
        binder.bind(pvs);

        // Then - print results for debugging
        System.out.println("=== Test: shouldBindMultipleOperations ===");
        System.out.println("Binding result errors: " + binder.getBindingResult().getAllErrors());
        System.out.println("Name filter: " + criteria.getName());
        if (criteria.getName() != null) {
            System.out.println("Name.contains: " + criteria.getName().getContains());
            System.out.println("Name.startsWith: " + criteria.getName().getStartsWith());
        }
        System.out.println();
        Assertions.assertNotNull(criteria.getName());
    }

    @Test
    @DisplayName("Should bind StringFilter with collection operation")
    void shouldBindCollectionOperation() {
        // Given - simulating: ?name.in=john,jane,bob
        UserCriteria criteria = new UserCriteria();
        WebDataBinder binder = new WebDataBinder(criteria);
        binder.setAutoGrowNestedPaths(true);
        editorRegistrar.registerCustomEditors(binder);

        MutablePropertyValues pvs = new MutablePropertyValues();
        pvs.add("name.in", "john,jane,bob");

        // When
        binder.bind(pvs);

        // Then - print results for debugging
        System.out.println("=== Test: shouldBindCollectionOperation ===");
        System.out.println("Binding result errors: " + binder.getBindingResult().getAllErrors());
        System.out.println("Name filter: " + criteria.getName());
        if (criteria.getName() != null) {
            System.out.println("Name.in: " + criteria.getName().getIn());
        }
        System.out.println();

        Assertions.assertNotNull(criteria.getName());
    }

    @Test
    @DisplayName("Should bind multiple StringFilter fields")
    void shouldBindMultipleFields() {
        // Given - simulating: ?name.eq=john&email.cont=@example.com
        UserCriteria criteria = new UserCriteria();
        WebDataBinder binder = new WebDataBinder(criteria);
        binder.setAutoGrowNestedPaths(true);
        editorRegistrar.registerCustomEditors(binder);

        MutablePropertyValues pvs = new MutablePropertyValues();
        pvs.add("name.eq", "john");
        pvs.add("email.cont", "@example.com");

        // When
        binder.bind(pvs);

        // Then - print results for debugging
        System.out.println("=== Test: shouldBindMultipleFields ===");
        System.out.println("Binding result errors: " + binder.getBindingResult().getAllErrors());
        System.out.println("Name filter: " + criteria.getName());
        if (criteria.getName() != null) {
            System.out.println("Name.equals: " + criteria.getName().getEquals());
        }
        System.out.println("Email filter: " + criteria.getEmail());
        if (criteria.getEmail() != null) {
            System.out.println("Email.contains: " + criteria.getEmail().getContains());
        }
        System.out.println();
        Assertions.assertNotNull(criteria.getName());
        Assertions.assertNotNull(criteria.getEmail());

    }

    /**
     * Test DTO simulating a real controller parameter.
     */
    static class UserCriteria {
        private StringFilter name;
        private StringFilter email;

        public StringFilter getName() {
            return name;
        }

        public void setName(StringFilter name) {
            this.name = name;
        }

        public StringFilter getEmail() {
            return email;
        }

        public void setEmail(StringFilter email) {
            this.email = email;
        }
    }
}
