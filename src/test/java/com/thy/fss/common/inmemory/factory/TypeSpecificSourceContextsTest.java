package com.thy.fss.common.inmemory.factory;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.engine.mapping.PropertyNavigation;
import com.thy.fss.common.inmemory.factory.source.BooleanSourceMappingContext;
import com.thy.fss.common.inmemory.factory.source.NumericSourceMappingContext;
import com.thy.fss.common.inmemory.factory.source.StringSourceMappingContext;
import com.thy.fss.common.inmemory.factory.target.PropertyNavigationContext;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.specification.attribute.ModelAttribute;
import com.thy.fss.common.inmemory.testmodel.Order;
import com.thy.fss.common.inmemory.testmodel.OrderSpecificationService;
import com.thy.fss.common.inmemory.testmodel.User;
import com.thy.fss.common.inmemory.testmodel.UserSpecificationService;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification tests for type-specific source mapping contexts.
 * Tests NumericSourceMappingContext, ComparableSourceMappingContext,
 * StringSourceMappingContext, and BooleanSourceMappingContext.
 */
@DisplayName("Type-Specific Source Contexts Verification")
class TypeSpecificSourceContextsTest {

    private TestRootBuilder rootBuilder;
    private PropertyNavigation primaryKeyPath;
    private PropertyNavigation foreignKeyPath;

    @BeforeEach
    void setUp() {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        rootBuilder = new TestRootBuilder(factory, UserSpecificationService.INSTANCE, "test-consumer-456", false);

        InMemoryDataSource<Order> orderDataSource = new InMemoryDataSource<>("order", Order.class);
        factory.registerDataSource("order", orderDataSource, Duration.ofMinutes(5));

        ModelAttribute<User, Long> userIdAttr = new ModelAttribute<>("id", User.class, Long.class);
        ModelAttribute<Order, Long> orderCustomerIdAttr = new ModelAttribute<>("customerId", Order.class, Long.class);

        primaryKeyPath = new PropertyNavigation(List.of(userIdAttr), List.of());
        foreignKeyPath = new PropertyNavigation(List.of(orderCustomerIdAttr), List.of());
    }

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    private static class TestRootBuilder extends AbstractRootBuilder<User> {
        TestRootBuilder(InMemorySpecStoreFactory factory, SpecificationService<User> entityClass, String consumerId, boolean isForDashboard) {
            super(factory, entityClass, consumerId, isForDashboard);
        }
    }

    @Nested
    @DisplayName("NumericSourceMappingContext Tests")
    class NumericSourceMappingContextTests {

        @Test
        @DisplayName("sum() should create mapping with SUM aggregation")
        void testSumCreatesMappingWithSumAggregation() {
            LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
            targetPath.add(new CollectionAttribute<>("totalSpent", User.class, BigDecimal.class));

            PropertyNavigationContext<User, BigDecimal> targetContext = new PropertyNavigationContext<>(
                    rootBuilder, targetPath, BigDecimal.class, new ArrayList<>()
            );

            NumericSourceMappingContext<User, BigDecimal, Order> context =
                    new NumericSourceMappingContext<>(targetContext, OrderSpecificationService.INSTANCE, Collections.singletonList(primaryKeyPath), Collections.singletonList(foreignKeyPath));

            // Change from CollectionAttribute to ModelAttribute
            ModelAttribute<Order, BigDecimal> amountAttr = new ModelAttribute<>("amount", Order.class, BigDecimal.class);
            AbstractRootBuilder<User> result = context.sum(nav -> nav.field(amountAttr));

            assertThat(result).isSameAs(rootBuilder);
            assertThat(rootBuilder.getPropertyMappings()).hasSize(1);

            PropertyMapping<User, ?> mapping = rootBuilder.getPropertyMappings().get(0);
            assertThat(mapping.getMappingType()).isEqualTo(MappingType.MANY_TO_ONE_AGGREGATION);
            assertThat(mapping.getAggregationType()).isEqualTo(AggregationType.SUM);
        }


        @Test
        @DisplayName("avg() should create mapping with AVG aggregation")
        void testAvgCreatesMappingWithAvgAggregation() {
            LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
            targetPath.add(new CollectionAttribute<>("avgOrderAmount", User.class, BigDecimal.class));

            PropertyNavigationContext<User, BigDecimal> targetContext = new PropertyNavigationContext<>(
                    rootBuilder, targetPath, BigDecimal.class, new ArrayList<>()
            );

            NumericSourceMappingContext<User, BigDecimal, Order> context =
                    new NumericSourceMappingContext<>(targetContext, OrderSpecificationService.INSTANCE, Collections.singletonList(primaryKeyPath), Collections.singletonList(foreignKeyPath));

            // Change from CollectionAttribute to ModelAttribute
            ModelAttribute<Order, BigDecimal> amountAttr = new ModelAttribute<>("amount", Order.class, BigDecimal.class);
            AbstractRootBuilder<User> result = context.avg(nav -> nav.field(amountAttr));

            assertThat(result).isSameAs(rootBuilder);
            assertThat(rootBuilder.getPropertyMappings()).hasSize(1);

            PropertyMapping<User, ?> mapping = rootBuilder.getPropertyMappings().get(0);
            assertThat(mapping.getMappingType()).isEqualTo(MappingType.MANY_TO_ONE_AGGREGATION);
            assertThat(mapping.getAggregationType()).isEqualTo(AggregationType.AVG);
        }


        @Test
        @DisplayName("sum() should validate source field is Number")
        void testSumValidatesSourceFieldIsNumber() {
            LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
            targetPath.add(new CollectionAttribute<>("totalSpent", User.class, BigDecimal.class));

            PropertyNavigationContext<User, BigDecimal> targetContext = new PropertyNavigationContext<>(
                    rootBuilder, targetPath, BigDecimal.class, new ArrayList<>()
            );

            NumericSourceMappingContext<User, BigDecimal, Order> context =
                    new NumericSourceMappingContext<>(targetContext, OrderSpecificationService.INSTANCE, Collections.singletonList(primaryKeyPath), Collections.singletonList(foreignKeyPath));

            ModelAttribute<Order, String> statusAttr = new ModelAttribute<>("status", Order.class, String.class);

            assertThatThrownBy(() -> context.sum(nav -> nav.field(statusAttr)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Source field type must be Number for sum operation");
        }
    }

    @Nested
    @DisplayName("ComparableSourceMappingContext Tests")
    class ComparableSourceMappingContextTests {
        // Tests commented out in original
    }

    @Nested
    @DisplayName("StringSourceMappingContext Tests")
    class StringSourceMappingContextTests {

        @Test
        @DisplayName("value() should validate source field is String")
        void testValueValidatesSourceFieldIsString() {
            LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
            targetPath.add(new ModelAttribute<>("name", User.class, String.class));

            PropertyNavigationContext<User, String> targetContext = new PropertyNavigationContext<>(
                    rootBuilder, targetPath, String.class, new ArrayList<>()
            );

            StringSourceMappingContext<User, Order> context =
                    new StringSourceMappingContext<>(targetContext, OrderSpecificationService.INSTANCE, Collections.singletonList(primaryKeyPath), Collections.singletonList(foreignKeyPath));

            ModelAttribute<Order, Long> idAttr = new ModelAttribute<>("id", Order.class, Long.class);

            assertThatThrownBy(() -> context.value(nav -> nav.field(idAttr)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Source field type must be String for value operation");
        }

        @Test
        @DisplayName("value() should create mapping when source field is String")
        void testValueCreatesMappingWhenSourceIsString() {
            LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
            targetPath.add(new ModelAttribute<>("name", User.class, String.class));

            PropertyNavigationContext<User, String> targetContext = new PropertyNavigationContext<>(
                    rootBuilder, targetPath, String.class, new ArrayList<>()
            );

            StringSourceMappingContext<User, Order> context =
                    new StringSourceMappingContext<>(targetContext, OrderSpecificationService.INSTANCE, Collections.singletonList(primaryKeyPath), Collections.singletonList(foreignKeyPath));

            ModelAttribute<Order, String> statusAttr = new ModelAttribute<>("status", Order.class, String.class);
            AbstractRootBuilder<User> result = context.value(nav -> nav.field(statusAttr));

            assertThat(result).isSameAs(rootBuilder);
            assertThat(rootBuilder.getPropertyMappings()).hasSize(1);

            PropertyMapping<User, ?> mapping = rootBuilder.getPropertyMappings().get(0);
            assertThat(mapping.getMappingType()).isEqualTo(MappingType.ONE_TO_ONE);
        }
    }

    @Nested
    @DisplayName("BooleanSourceMappingContext Tests")
    class BooleanSourceMappingContextTests {

        @Test
        @DisplayName("value() should validate source field is Boolean")
        void testValueValidatesSourceFieldIsBoolean() {
            LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
            targetPath.add(new ModelAttribute<>("isActive", User.class, Boolean.class));

            PropertyNavigationContext<User, Boolean> targetContext = new PropertyNavigationContext<>(
                    rootBuilder, targetPath, Boolean.class, new ArrayList<>()
            );

            BooleanSourceMappingContext<User, Order> context =
                    new BooleanSourceMappingContext<>(targetContext, OrderSpecificationService.INSTANCE, Collections.singletonList(primaryKeyPath), Collections.singletonList(foreignKeyPath));

            ModelAttribute<Order, String> statusAttr = new ModelAttribute<>("status", Order.class, String.class);

            assertThatThrownBy(() -> context.value(nav -> nav.field(statusAttr)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Source field type must be Boolean for value operation");
        }

        @Test
        @DisplayName("value() should create mapping when source field is Boolean")
        void testValueCreatesMappingWhenSourceIsBoolean() {
            LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
            targetPath.add(new ModelAttribute<>("isActive", User.class, Boolean.class));

            PropertyNavigationContext<User, Boolean> targetContext = new PropertyNavigationContext<>(
                    rootBuilder, targetPath, Boolean.class, new ArrayList<>()
            );

            BooleanSourceMappingContext<User, Order> context =
                    new BooleanSourceMappingContext<>(targetContext, OrderSpecificationService.INSTANCE, Collections.singletonList(primaryKeyPath), Collections.singletonList(foreignKeyPath));

            ModelAttribute<Order, Boolean> isPaidAttr = new ModelAttribute<>("isPaid", Order.class, Boolean.class);
            AbstractRootBuilder<User> result = context.value(nav -> nav.field(isPaidAttr));

            assertThat(result).isSameAs(rootBuilder);
            assertThat(rootBuilder.getPropertyMappings()).hasSize(1);

            PropertyMapping<User, ?> mapping = rootBuilder.getPropertyMappings().getFirst();
            assertThat(mapping.getMappingType()).isEqualTo(MappingType.ONE_TO_ONE);
        }
    }
}
