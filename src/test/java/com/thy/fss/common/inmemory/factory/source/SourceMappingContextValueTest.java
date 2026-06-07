package com.thy.fss.common.inmemory.factory.source;

import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.AbstractRootBuilder;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.factory.target.PropertyNavigationContext;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.Customer;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.Customer_;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.CustomerSpecificationService;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.Order;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.Order_;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying both the parameterless {@code value()} and the
 * parameterized {@code value(sourceFieldBuilder)} methods on
 * {@link SourceMappingContext}.
 */
@DisplayName("SourceMappingContext value() tests")
class SourceMappingContextValueTest {

    @Test
    @DisplayName("Parameterless value() should create PropertyMapping with ONE_TO_ONE type and null sourcePath")
    @SuppressWarnings("unchecked")
    void parameterlessValueShouldCreateOneToOneMappingWithNullSourcePath() {
        // Arrange: set up mocks for the dependency chain used by createMapping
        PropertyNavigationContext<Order, Customer> targetContext = mock(PropertyNavigationContext.class);
        AbstractRootBuilder<Order> rootBuilder = mock(AbstractRootBuilder.class);
        InMemorySpecStoreFactory factory = mock(InMemorySpecStoreFactory.class);

        // Wire the mock chain: targetContext -> rootBuilder -> factory
        when(targetContext.getRootBuilder()).thenReturn(rootBuilder);
        when(rootBuilder.getFactory()).thenReturn(factory);
        when(rootBuilder.getConsumerId()).thenReturn("test-store");
        when(rootBuilder.isForDashboard()).thenReturn(false);
        when(rootBuilder.getEntityClass()).thenReturn(Order.class);

        // Factory resolves datasource name for the source entity class
        when(factory.getDataSourceNameByClass(Customer.class)).thenReturn("customers");

        // Target path: a single-level path (e.g., Order_.customerId as placeholder)
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
        targetPath.add(Order_.customerId);
        when(targetContext.getTargetPath()).thenReturn(targetPath);
        when(targetContext.getCollectionOperations()).thenReturn(null);

        // PK/FK paths for the join relationship
        PropertyNavigation pkNav = mock(PropertyNavigation.class);
        PropertyNavigation fkNav = mock(PropertyNavigation.class);
        when(pkNav.getPath()).thenReturn(List.of(Order_.customerId));
        when(fkNav.getPath()).thenReturn(List.of(Order_.customerId));

        List<PropertyNavigation> primaryKeyPaths = Collections.singletonList(pkNav);
        List<PropertyNavigation> foreignKeyPaths = Collections.singletonList(fkNav);

        // Create the context under test
        SourceMappingContext<Order, Customer, Customer> context = new SourceMappingContext<>(
                targetContext,
                CustomerSpecificationService.INSTANCE,
                primaryKeyPaths,
                foreignKeyPaths
        );

        // Capture the PropertyMapping passed to addPropertyMapping
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<PropertyMapping> captor = ArgumentCaptor.forClass(PropertyMapping.class);

        // Act: call parameterless value()
        AbstractRootBuilder<Order> result = context.value();

        // Assert: verify addPropertyMapping was called and capture the argument
        verify(rootBuilder).addPropertyMapping(captor.capture());
        PropertyMapping<Order, Customer> capturedMapping = captor.getValue();

        assertThat(capturedMapping.getMappingType())
                .as("Parameterless value() should create a ONE_TO_ONE mapping")
                .isEqualTo(MappingType.ONE_TO_ONE);

        assertThat(capturedMapping.getSourcePath())
                .as("Parameterless value() should set sourcePath to null")
                .isNull();

        // Also verify it returns the root builder
        assertThat(result)
                .as("value() should return the root builder")
                .isSameAs(rootBuilder);
    }

    @Test
    @DisplayName("Regression: parameterized value(sourceFieldBuilder) should still create ONE_TO_ONE mapping with non-null sourcePath")
    @SuppressWarnings("unchecked")
    void parameterizedValueShouldStillCreateOneToOneMappingWithSourcePath() {
        // Arrange: same mock setup as the parameterless test
        PropertyNavigationContext<Order, String> targetContext = mock(PropertyNavigationContext.class);
        AbstractRootBuilder<Order> rootBuilder = mock(AbstractRootBuilder.class);
        InMemorySpecStoreFactory factory = mock(InMemorySpecStoreFactory.class);

        when(targetContext.getRootBuilder()).thenReturn(rootBuilder);
        when(rootBuilder.getFactory()).thenReturn(factory);
        when(rootBuilder.getConsumerId()).thenReturn("test-store");
        when(rootBuilder.isForDashboard()).thenReturn(false);
        when(rootBuilder.getEntityClass()).thenReturn(Order.class);

        when(factory.getDataSourceNameByClass(Customer.class)).thenReturn("customers");

        // Target path: Order_.customerCityName (a String field on Order)
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
        targetPath.add(Order_.customerCityName);
        when(targetContext.getTargetPath()).thenReturn(targetPath);
        when(targetContext.getCollectionOperations()).thenReturn(null);

        // PK/FK paths
        PropertyNavigation pkNav = mock(PropertyNavigation.class);
        PropertyNavigation fkNav = mock(PropertyNavigation.class);
        when(pkNav.getPath()).thenReturn(List.of(Order_.customerId));
        when(fkNav.getPath()).thenReturn(List.of(Customer_.id));

        List<PropertyNavigation> primaryKeyPaths = Collections.singletonList(pkNav);
        List<PropertyNavigation> foreignKeyPaths = Collections.singletonList(fkNav);

        SourceMappingContext<Order, String, Customer> context = new SourceMappingContext<>(
                targetContext,
                CustomerSpecificationService.INSTANCE,
                primaryKeyPaths,
                foreignKeyPaths
        );

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<PropertyMapping> captor = ArgumentCaptor.forClass(PropertyMapping.class);

        // Act: call parameterized value() selecting Customer_.name
        AbstractRootBuilder<Order> result = context.value(nav -> nav.field(Customer_.name));

        // Assert
        verify(rootBuilder).addPropertyMapping(captor.capture());
        PropertyMapping<Order, String> capturedMapping = captor.getValue();

        assertThat(capturedMapping.getMappingType())
                .as("Parameterized value() should create a ONE_TO_ONE mapping")
                .isEqualTo(MappingType.ONE_TO_ONE);

        assertThat(capturedMapping.getSourcePath())
                .as("Parameterized value() should set a non-null sourcePath reflecting the selected field")
                .isNotNull()
                .isNotEmpty();

        // Verify the sourcePath correctly reflects Customer_.name
        assertThat(capturedMapping.getSourcePath().get(0).getName())
                .as("sourcePath should point to the 'name' field of Customer")
                .isEqualTo("name");

        assertThat(result)
                .as("value(sourceFieldBuilder) should return the root builder")
                .isSameAs(rootBuilder);
    }
}
