package com.thy.fss.common.inmemory.factory;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.common.TestSynchronizationHelper;
import com.thy.fss.common.inmemory.common.model.TestDashboardSpecificationService;
import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.testmodel.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.OrderSpecificationService;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.CustomerSpecificationService;
import com.thy.fss.common.inmemory.testmodel.UserDashboardSpecificationService;
import com.thy.fss.common.inmemory.testmodel.SalesDashboardSpecificationService;
import com.thy.fss.common.inmemory.specification.SpecificationService;

/**
 * Comprehensive test coverage for DashboardBuilder with new API.
 * Tests dashboard configuration, build process, and validation.
 */
class DashboardBuilderTest {
    private static final String NULL_ATTRIBUTE_MSG = "Target attribute cannot be null";
    private static final String NO_MAPPINGS_MSG = "Dashboard must have at least one property mapping";
    private static final String CUSTOM_DASHBOARD_NAME = "Sales Analytics Dashboard";
    private static final String ORDERS_DATASOURCE = "orders";
    private static final String CUSTOMERS_DATASOURCE = "customers";
    private static final String PRODUCTS_DATASOURCE = "products";
    private static final String DEFAULT_DASHBOARD_SUFFIX = "-Dashboard";

    private InMemorySpecStoreFactory factory;
    private InMemoryDataSource<Customer> customerDataSource;
    private InMemoryDataSource<Order> orderDataSource;
    private InMemoryDataSource<Product> productDataSource;
    private List<Customer> testCustomers;
    private List<Order> testOrders;
    private List<Product> testProducts;
    private DataSynchronizationEngine engine;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        
        LargeDatasetGenerator generator = new LargeDatasetGenerator();
        testCustomers = generator.generateCustomers(10_000);
        testOrders = generator.generateOrders(10_000, 5);
        testProducts = generator.generateProducts(1_000);

        customerDataSource = new InMemoryDataSource<>(CUSTOMERS_DATASOURCE, Customer.class, testCustomers);
        orderDataSource = new InMemoryDataSource<>(ORDERS_DATASOURCE, Order.class, testOrders);
        productDataSource = new InMemoryDataSource<>(PRODUCTS_DATASOURCE, Product.class, testProducts);

        factory.registerDataSource(Customer.class, customerDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(Order.class, orderDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(Product.class, productDataSource, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
        if (customerDataSource != null) {
            customerDataSource.close();
        }
        if (orderDataSource != null) {
            orderDataSource.close();
        }
        if (productDataSource != null) {
            productDataSource.close();
        }
    }

    @Test
    void testValidationNullTargetClass() {
        assertThatThrownBy(() -> new DashboardBuilder<>(factory, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Target service cannot be null");
    }

    @Test
    void testValidationNullFactory() {
        assertThatThrownBy(() -> new DashboardBuilder<>(null, SalesDashboardSpecificationService.INSTANCE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Factory cannot be null");
    }

    @Test
    void testBuildProcessUniqueDashboardIds() {
        DashboardBuilder<SalesDashboard> builder1 = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);
        Dashboard<SalesDashboard> dashboard1 = buildDashboard(builder1, ORDERS_DATASOURCE);

        DashboardBuilder<SalesDashboard> builder2 = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);
        Dashboard<SalesDashboard> dashboard2 = buildDashboard(builder2, ORDERS_DATASOURCE);

        assertThat(dashboard1.getId()).isNotEqualTo(dashboard2.getId());
    }

    @Test
    void testConfigurationOptionsMinimalConfiguration() {
        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);
        Dashboard<SalesDashboard> dashboard = buildDashboard(builder, ORDERS_DATASOURCE);

        assertThat(dashboard).isNotNull();
        assertThat(dashboard.getTargetClass()).isEqualTo(SalesDashboard.class);
        assertThat(dashboard.getName()).isEqualTo("SalesDashboard" + DEFAULT_DASHBOARD_SUFFIX);
    }

    @Test
    void testConfigurationOptionsWithCustomName() {
        String name = "My Sales Dashboard";
        DashboardBuilder<SalesDashboard> builder = factory
                .buildDashboard(SalesDashboardSpecificationService.INSTANCE)
                .withName(name);

        Dashboard<SalesDashboard> dashboard = buildDashboard(builder, ORDERS_DATASOURCE);
        assertThat(dashboard).isNotNull();
        assertThat(dashboard.getName()).isEqualTo(name);
    }

    @Test
    void testConfigurationOptionsLargeDataset() {
        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);
        Dashboard<SalesDashboard> dashboard = buildDashboard(builder, CUSTOMERS_DATASOURCE);

        assertThat(dashboard).isNotNull();
        assertThat(testCustomers).hasSize(10_000);
        assertThat(testOrders).hasSizeGreaterThanOrEqualTo(10_000);
        assertThat(testProducts).hasSize(1_000);
    }



    private Dashboard<SalesDashboard> buildDashboard(DashboardBuilder<SalesDashboard> builder, String dashboardName) {
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();

        targetPath.add(SalesDashboard_.totalOrders);

        PropertyMapping<SalesDashboard, Long> mapping = PropertyMapping.<SalesDashboard, Long>builder()
                .consumerId("test-consumer-456")
                .datasourceName(dashboardName)
                .isForDashboard(true)
                .targetPath(targetPath)
                .sourceService(SalesDashboardSpecificationService.INSTANCE)
                .targetService(SalesDashboardSpecificationService.INSTANCE)
                .sourcePath(null)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        builder.addPropertyMapping(mapping);
        return builder.build();
    }

    @Test
    @DisplayName("Should handle numeric target with Double attribute")
    void shouldHandleNumericTargetWithDoubleAttribute() {
        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);

        Dashboard<SalesDashboard> dashboard = buildNumericDashboard(
                builder,
                SalesDashboard_.totalRevenue,
                AggregationType.SUM
        );

        assertThat(dashboard).isNotNull();
    }

    @Test
    @DisplayName("Should reject null Long attribute in target")
    void shouldRejectNullLongAttributeInTarget() {
        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);

        assertThatThrownBy(() -> builder.target((com.thy.fss.common.inmemory.specification.attribute.LongAttribute<SalesDashboard>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
    }

    @Test
    @DisplayName("Should reject null Integer attribute in target")
    void shouldRejectNullIntegerAttributeInTarget() {
        DashboardBuilder<com.thy.fss.common.inmemory.common.model.TestDashboard> builder = factory.buildDashboard(TestDashboardSpecificationService.INSTANCE);

        assertThatThrownBy(() -> builder.target((com.thy.fss.common.inmemory.specification.attribute.IntegerAttribute<com.thy.fss.common.inmemory.common.model.TestDashboard>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
    }

    @Test
    @DisplayName("Should reject null Double attribute in target")
    void shouldRejectNullDoubleAttributeInTarget() {
        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);

        assertThatThrownBy(() -> builder.target((com.thy.fss.common.inmemory.specification.attribute.DoubleAttribute<SalesDashboard>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
    }


    @Test
    @DisplayName("Should reject null String attribute in target")
    void shouldRejectNullStringAttributeInTarget() {
        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);

        assertThatThrownBy(() -> builder.target((com.thy.fss.common.inmemory.specification.attribute.StringAttribute<SalesDashboard>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
    }

    @Test
    @DisplayName("Should reject null Boolean attribute in target")
    void shouldRejectNullBooleanAttributeInTarget() {
        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);

        assertThatThrownBy(() -> builder.target((com.thy.fss.common.inmemory.specification.attribute.BooleanAttribute<SalesDashboard>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
    }

    @Test
    @org.junit.jupiter.api.Disabled("ComplexDashboard class does not exist")
    @DisplayName("Should handle Model target attribute")
    void shouldHandleModelTargetAttribute() {
        /* DISABLED
        DashboardBuilder<ComplexDashboard> builder = factory.buildDashboard(ComplexDashboardSpecificationService.INSTANCE);

        Dashboard<ComplexDashboard> dashboard = buildModelDashboard(builder);

        assertThat(dashboard).isNotNull();
        */
    }

    @Test
    @org.junit.jupiter.api.Disabled("ComplexDashboard class does not exist")
    @DisplayName("Should reject null Model attribute in target")
    void shouldRejectNullModelAttributeInTarget() {
        /* DISABLED
        DashboardBuilder<ComplexDashboard> builder = factory.buildDashboard(ComplexDashboardSpecificationService.INSTANCE);

        assertThatThrownBy(() -> builder.target((com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<ComplexDashboard, CustomerStats>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
        */
    }

    @Test
    @org.junit.jupiter.api.Disabled("ComplexDashboard class does not exist")
    @DisplayName("Should reject null Collection attribute in target")
    void shouldRejectNullCollectionAttributeInTarget() {
        /* DISABLED
        DashboardBuilder<ComplexDashboard> builder = factory.buildDashboard(ComplexDashboardSpecificationService.INSTANCE);

        assertThatThrownBy(() -> builder.target((com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute<ComplexDashboard, String>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(NULL_ATTRIBUTE_MSG);
        */
    }

    @Test
    @DisplayName("Should throw exception when building without property mappings")
    void shouldThrowExceptionWhenBuildingWithoutPropertyMappings() {
        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(NO_MAPPINGS_MSG);
    }

    @Test
    @DisplayName("Should allow multiple target mappings in single dashboard")
    void shouldAllowMultipleTargetMappings() {
        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);

        addNumericMapping(builder, SalesDashboard_.totalOrders, AggregationType.COUNT);
        addNumericMapping(builder, SalesDashboard_.totalRevenue, AggregationType.SUM);

        Dashboard<SalesDashboard> dashboard = builder.build();

        // Initialize engine
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        TestSynchronizationHelper.waitForCondition(
                () -> dashboard.getData() != null,
                Duration.ofSeconds(5)
        );

        assertThat(dashboard).isNotNull();
        assertThat(dashboard.getData()).isNotNull();
    }

    @Test
    @DisplayName("Should return builder when setting name with withName")
    void shouldReturnBuilderWhenSettingNameWithWithName() {
        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);
        DashboardBuilder<SalesDashboard> result = builder.withName(CUSTOM_DASHBOARD_NAME);

        assertThat(result).isSameAs(builder);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Should keep default name when invalid name provided")
    void shouldKeepDefaultNameWhenInvalidNameProvided(String invalidName) {
        Dashboard<SalesDashboard> dashboard = buildDashboardWithMapping(
                factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE).withName(invalidName)
        );

        assertThat(dashboard.getName()).isEqualTo("SalesDashboard" + DEFAULT_DASHBOARD_SUFFIX);
    }


    @Test
    @DisplayName("Should register dashboard with factory during build")
    void shouldRegisterDashboardWithFactoryDuringBuild() {
        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);
        Dashboard<SalesDashboard> dashboard = buildDashboardWithMapping(builder);

        assertThat(dashboard).isNotNull();
        assertThat(dashboard.getId()).isNotNull();
    }

    @Test
    @DisplayName("Should handle dashboard with AVG aggregation")
    void shouldHandleDashboardWithAvgAggregation() {
        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);
        Dashboard<SalesDashboard> dashboard = buildNumericDashboard(
                builder,
                SalesDashboard_.averageOrderValue,
                AggregationType.AVG
        );

        assertThat(dashboard).isNotNull();
    }

    @Test
    @DisplayName("Should handle dashboard with MIN aggregation")
    void shouldHandleDashboardWithMinAggregation() {
        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);
        Dashboard<SalesDashboard> dashboard = buildNumericDashboard(
                builder,
                SalesDashboard_.totalOrders,
                AggregationType.MIN
        );

        assertThat(dashboard).isNotNull();
    }

    @Test
    @DisplayName("Should handle dashboard with MAX aggregation")
    void shouldHandleDashboardWithMaxAggregation() {
        DashboardBuilder<SalesDashboard> builder = factory.buildDashboard(SalesDashboardSpecificationService.INSTANCE);
        Dashboard<SalesDashboard> dashboard = buildNumericDashboard(
                builder,
                SalesDashboard_.totalRevenue,
                AggregationType.MAX
        );

        assertThat(dashboard).isNotNull();
    }

    // New tests for coverage of property navigation contexts

    @Test
    @org.junit.jupiter.api.Disabled("StringDashboard class does not exist")
    @DisplayName("Should handle StringPropertyNavigationContext with String attribute")
    void shouldHandleStringPropertyNavigationContext() {
        /* DISABLED
        DashboardBuilder<StringDashboard> builder = factory.buildDashboard(StringDashboardSpecificationService.INSTANCE);
        addStringMapping(builder, StringDashboard.topCustomerName);

        Dashboard<StringDashboard> dashboard = builder.build();

        assertThat(dashboard).isNotNull();
        */
    }

    @Test
    @org.junit.jupiter.api.Disabled("BooleanDashboard class does not exist")
    @DisplayName("Should handle BooleanPropertyNavigationContext with Boolean attribute")
    void shouldHandleBooleanPropertyNavigationContext() {
        /* DISABLED
        DashboardBuilder<BooleanDashboard> builder = factory.buildDashboard(BooleanDashboardSpecificationService.INSTANCE);
        addBooleanMapping(builder, BooleanDashboard.hasActiveOrders);

        Dashboard<BooleanDashboard> dashboard = builder.build();

        assertThat(dashboard).isNotNull();
        */
    }

    @Test
    @org.junit.jupiter.api.Disabled("EnumDashboard class does not exist")
    @DisplayName("Should handle EnumPropertyNavigationContext with Enum attribute")
    void shouldHandleEnumPropertyNavigationContext() {
        /* DISABLED
        DashboardBuilder<EnumDashboard> builder = factory.buildDashboard(EnumDashboardSpecificationService.INSTANCE);
        addEnumMapping(builder, EnumDashboard.orderStatus);

        Dashboard<EnumDashboard> dashboard = builder.build();

        assertThat(dashboard).isNotNull();
        */
    }


    @Test
    @org.junit.jupiter.api.Disabled("CollectionDashboard class does not exist")
    @DisplayName("Should handle CollectionPropertyNavigationContext with Collection attribute")
    void shouldHandleCollectionPropertyNavigationContext() {
        /* DISABLED
        DashboardBuilder<CollectionDashboard> builder = factory.buildDashboard(CollectionDashboardSpecificationService.INSTANCE);
        addCollectionMapping(builder, CollectionDashboard.recentOrderIds);

        Dashboard<CollectionDashboard> dashboard = builder.build();

        assertThat(dashboard).isNotNull();
        */
    }

    @Test
    @org.junit.jupiter.api.Disabled("ComplexDashboard class does not exist")
    @DisplayName("Should handle ModelPropertyNavigationContext with nested Model attribute")
    void shouldHandleModelPropertyNavigationContextWithNestedFields() {
        /* DISABLED
        DashboardBuilder<ComplexDashboard> builder = factory.buildDashboard(ComplexDashboardSpecificationService.INSTANCE);
        addNestedModelMapping(builder, ComplexDashboard_.customerStats);

        Dashboard<ComplexDashboard> dashboard = builder.build();

        assertThat(dashboard).isNotNull();
        */
    }

    // Helper methods
    private Dashboard<SalesDashboard> buildDashboardWithMapping(DashboardBuilder<SalesDashboard> builder) {
        addNumericMapping(builder, SalesDashboard_.totalOrders, AggregationType.COUNT);
        return builder.build();
    }

    private <N extends Number> Dashboard<SalesDashboard> buildNumericDashboard(
            DashboardBuilder<SalesDashboard> builder,
            com.thy.fss.common.inmemory.specification.attribute.NumericMetaAttribute<SalesDashboard, N> attribute,
            AggregationType aggregationType) {
        addNumericMapping(builder, attribute, aggregationType);
        return builder.build();
    }

    /* DISABLED: Non-existent Dashboard classes - missing SpecificationService generation
    private Dashboard<ComplexDashboard> buildModelDashboard(DashboardBuilder<ComplexDashboard> builder) {
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
        LinkedList<MetaAttribute<?, ?>> sourcePath = new LinkedList<>();
        targetPath.add(ComplexDashboard_.customerStats);
        sourcePath.add(CustomerStats_.totalSpent);
        PropertyMapping<ComplexDashboard, Double> mapping = PropertyMapping.<ComplexDashboard, Double>builder()
                .consumerId(builder.getConsumerId())
                .datasourceName(ORDERS_DATASOURCE)
                .isForDashboard(true)
                .targetPath(targetPath)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserDashboardSpecificationService.INSTANCE)
                .sourcePath(sourcePath)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        builder.addPropertyMapping(mapping);
        return builder.build();
    }
    */

    private <N extends Number> void addNumericMapping(
            DashboardBuilder<SalesDashboard> builder,
            com.thy.fss.common.inmemory.specification.attribute.NumericMetaAttribute<SalesDashboard, N> attribute,
            AggregationType aggregationType) {
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
        targetPath.add(attribute);

        LinkedList<MetaAttribute<?, ?>> sourcePath = new LinkedList<>();
        sourcePath.add(Order_.id);

        PropertyMapping<SalesDashboard, N> mapping = PropertyMapping.<SalesDashboard, N>builder()
                .consumerId(builder.getConsumerId())
                .datasourceName(ORDERS_DATASOURCE)
                .isForDashboard(true)
                .targetPath(targetPath)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(SalesDashboardSpecificationService.INSTANCE)
                .sourcePath(sourcePath)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(aggregationType)
                .build();

        builder.addPropertyMapping(mapping);
    }

    /* DISABLED: Non-existent Dashboard classes - missing SpecificationService generation
    private void addStringMapping(
            DashboardBuilder<StringDashboard> builder,
            com.thy.fss.common.inmemory.specification.attribute.StringAttribute<StringDashboard> attribute) {
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
        targetPath.add(attribute);

        LinkedList<MetaAttribute<?, ?>> sourcePath = new LinkedList<>();
        sourcePath.add(Customer_.name);

        PropertyMapping<StringDashboard, String> mapping = PropertyMapping.<StringDashboard, String>builder()
                .consumerId(builder.getConsumerId())
                .datasourceName(CUSTOMERS_DATASOURCE)
                .isForDashboard(true)
                .targetPath(targetPath)
                .sourceService(CustomerSpecificationService.INSTANCE)
                .targetService(UserDashboardSpecificationService.INSTANCE)
                .sourcePath(sourcePath)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        builder.target(attribute);
        builder.addPropertyMapping(mapping);
    }
    */

    /* DISABLED: Non-existent Dashboard classes - missing SpecificationService generation
    private void addBooleanMapping(
            DashboardBuilder<BooleanDashboard> builder,
            com.thy.fss.common.inmemory.specification.attribute.BooleanAttribute<BooleanDashboard> attribute) {
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
        targetPath.add(attribute);

        PropertyMapping<BooleanDashboard, Boolean> mapping = PropertyMapping.<BooleanDashboard, Boolean>builder()
                .consumerId(builder.getConsumerId())
                .datasourceName(ORDERS_DATASOURCE)
                .isForDashboard(true)
                .targetPath(targetPath)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserDashboardSpecificationService.INSTANCE)
                .sourcePath(null)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        builder.target(attribute);
        builder.addPropertyMapping(mapping);
    }
    */

    /* DISABLED: Non-existent Dashboard classes - missing SpecificationService generation
    private void addEnumMapping(
            DashboardBuilder<EnumDashboard> builder,
            com.thy.fss.common.inmemory.specification.attribute.EnumAttribute<EnumDashboard, OrderStatus> attribute) {
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
        targetPath.add(attribute);

        LinkedList<MetaAttribute<?, ?>> sourcePath = new LinkedList<>();
        sourcePath.add(Order_.status);

        PropertyMapping<EnumDashboard, OrderStatus> mapping = PropertyMapping.<EnumDashboard, OrderStatus>builder()
                .consumerId(builder.getConsumerId())
                .datasourceName(ORDERS_DATASOURCE)
                .isForDashboard(true)
                .targetPath(targetPath)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserDashboardSpecificationService.INSTANCE)
                .sourcePath(sourcePath)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        builder.addPropertyMapping(mapping);
    }
    */

    /* DISABLED: Non-existent Dashboard classes - missing SpecificationService generation
    private void addCollectionMapping(
            DashboardBuilder<CollectionDashboard> builder,
            com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute<CollectionDashboard, Long> attribute) {
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
        targetPath.add(attribute);

        LinkedList<MetaAttribute<?, ?>> sourcePath = new LinkedList<>();
        sourcePath.add(Order_.id);

        PropertyMapping<CollectionDashboard, Long> mapping = PropertyMapping.<CollectionDashboard, Long>builder()
                .consumerId(builder.getConsumerId())
                .datasourceName(ORDERS_DATASOURCE)
                .isForDashboard(true)
                .targetPath(targetPath)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserDashboardSpecificationService.INSTANCE)
                .sourcePath(sourcePath)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .build();

        builder.target(attribute);
        builder.addPropertyMapping(mapping);
    }
    */

    /* DISABLED: Non-existent Dashboard classes - missing SpecificationService generation
    private void addNestedModelMapping(
            DashboardBuilder<ComplexDashboard> builder,
            com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<ComplexDashboard, CustomerStats> attribute) {
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
        LinkedList<MetaAttribute<?, ?>> sourcePath = new LinkedList<>();

        targetPath.add(attribute);
        targetPath.add(CustomerStats_.totalSpent);

        sourcePath.add(Order_.totalAmount);

        PropertyMapping<ComplexDashboard, Double> mapping = PropertyMapping.<ComplexDashboard, Double>builder()
                .consumerId(builder.getConsumerId())
                .datasourceName(ORDERS_DATASOURCE)
                .isForDashboard(true)
                .targetPath(targetPath)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserDashboardSpecificationService.INSTANCE)
                .sourcePath(sourcePath)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .build();

        builder.addPropertyMapping(mapping);
        builder.target(attribute);
    }
    */

    // Test model classes for new navigation contexts
    /* DISABLED: Non-existent Dashboard classes - missing SpecificationService generation

    private static class StringDashboard {
        public static final com.thy.fss.common.inmemory.specification.attribute.StringAttribute<StringDashboard> topCustomerName =
                new com.thy.fss.common.inmemory.specification.attribute.StringAttribute<>("topCustomerName", StringDashboard.class);
    }


    private static class BooleanDashboard {
        public static final com.thy.fss.common.inmemory.specification.attribute.BooleanAttribute<BooleanDashboard> hasActiveOrders =
                new com.thy.fss.common.inmemory.specification.attribute.BooleanAttribute<>("hasActiveOrders", BooleanDashboard.class);
    }


    private static class EnumDashboard {
        public static final com.thy.fss.common.inmemory.specification.attribute.EnumAttribute<EnumDashboard, OrderStatus> orderStatus =
                new com.thy.fss.common.inmemory.specification.attribute.EnumAttribute<>("orderStatus", EnumDashboard.class, OrderStatus.class);
    }

    private static class ComparableDashboard {
        private LocalDateTime lastOrderDate;

        public LocalDateTime getLastOrderDate() {
            return lastOrderDate;
        }

        public void setLastOrderDate(LocalDateTime lastOrderDate) {
            this.lastOrderDate = lastOrderDate;
        }
    }

    private static class CollectionDashboard {
        public static final com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute<CollectionDashboard, Long> recentOrderIds =
                new com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute<>("recentOrderIds", CollectionDashboard.class, Long.class);
    }
    */

    // Existing test model classes
    private static class TestDashboard {
        private Integer integerCount;

        public Integer getIntegerCount() {
            return integerCount;
        }

        public void setIntegerCount(Integer integerCount) {
            this.integerCount = integerCount;
        }
    }

    private static class TestDashboard_ {
        public static final com.thy.fss.common.inmemory.specification.attribute.IntegerAttribute<TestDashboard> integerCount =
                new com.thy.fss.common.inmemory.specification.attribute.IntegerAttribute<>("integerCount", TestDashboard.class);
    }

    private static class ComplexDashboard {
        private CustomerStats customerStats;
        private List<String> topProducts;

        public CustomerStats getCustomerStats() {
            return customerStats;
        }

        public void setCustomerStats(CustomerStats customerStats) {
            this.customerStats = customerStats;
        }

        public List<String> getTopProducts() {
            return topProducts;
        }

        public void setTopProducts(List<String> topProducts) {
            this.topProducts = topProducts;
        }
    }

    private static class ComplexDashboard_ {
        public static final com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<ComplexDashboard, CustomerStats> customerStats =
                new com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<>("customerStats", ComplexDashboard.class, CustomerStats.class);
        public static final com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute<ComplexDashboard, String> topProducts =
                new com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute<>("topProducts", ComplexDashboard.class, String.class);
    }

    private static class CustomerStats {
        private Double totalSpent;

        public Double getTotalSpent() {
            return totalSpent;
        }

        public void setTotalSpent(Double totalSpent) {
            this.totalSpent = totalSpent;
        }
    }

    private static class CustomerStats_ {
        public static final com.thy.fss.common.inmemory.specification.attribute.DoubleAttribute<CustomerStats> totalSpent =
                new com.thy.fss.common.inmemory.specification.attribute.DoubleAttribute<>("totalSpent", CustomerStats.class);
    }

    private enum OrderStatus {
        PENDING, ACTIVE, COMPLETED, CANCELLED
    }
}
