package com.thy.fss.common.inmemory.integration.endtoend;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.factory.DashboardBuilder;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.testmodel.dashboard.SimpleDashboard;
import com.thy.fss.common.inmemory.testmodel.dashboard.SimpleDashboardSpecificationService;
import com.thy.fss.common.inmemory.testmodel.dashboard.SimpleDashboard_;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.Order;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.OrderSpecificationService;
import com.thy.fss.common.inmemory.testmodel.nestedmapping.Order_;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive end-to-end test for Dashboard aggregations.
 * Tests nested source field aggregation, multiple dashboard mappings,
 * and common aggregation key reuse.
 */
@DisplayName("Dashboard Aggregation End-to-End Test")
class DashboardAggregationEndToEndTest {

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;
    private Dashboard<SimpleDashboard> dashboard;
    private InMemoryDataSource<Order> orderDataSource;

    @BeforeEach
    void setUp() {
        DataSyncTestHelper.clearStaticRegistries();
        factory = InMemorySpecStoreFactory.getInstance();
    }

    @AfterEach
    void tearDown() {
        if (engine != null) {
            try {
                engine.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (orderDataSource != null) {
            try {
                orderDataSource.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    @DisplayName("Should aggregate with nested source fields")
    void testNestedSourceFieldAggregation() {
        List<Order> orders = createOrderData();

        orderDataSource = new InMemoryDataSource<>("orders", Order.class, orders);
        factory.registerDataSource("orders", orderDataSource, Duration.ofSeconds(10));

        DashboardBuilder<SimpleDashboard> stepBuilder = (DashboardBuilder<SimpleDashboard>) factory
                .buildDashboard(SimpleDashboardSpecificationService.INSTANCE)
                .withName("simple-dashboard");
        stepBuilder = (DashboardBuilder<SimpleDashboard>) stepBuilder
                .target(SimpleDashboard_.totalCount)
                    .from(OrderSpecificationService.INSTANCE, pk -> pk.field(Order_.id), fk -> fk.field(Order_.id))
                    .count();
        stepBuilder = (DashboardBuilder<SimpleDashboard>) stepBuilder
                .target(SimpleDashboard_.totalAmount)
                    .from(OrderSpecificationService.INSTANCE, pk -> pk.field(Order_.id), fk -> fk.field(Order_.id))
                    .sum(sfb -> sfb.field(Order_.totalAmount));
        stepBuilder = (DashboardBuilder<SimpleDashboard>) stepBuilder
                .target(SimpleDashboard_.averageAmount)
                    .from(OrderSpecificationService.INSTANCE, pk -> pk.field(Order_.id), fk -> fk.field(Order_.id))
                    .avg(sfb -> sfb.field(Order_.totalAmount));
        dashboard = stepBuilder.build();

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        DataSyncTestHelper.awaitSync(() -> dashboard.getData() != null, Duration.ofSeconds(5));

        SimpleDashboard result = dashboard.getData();
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(5);
        assertThat(result.getTotalAmount()).isEqualTo(1500.0);
        assertThat(result.getAverageAmount()).isEqualTo(300.0);
    }

    @Test
    @DisplayName("Should reuse common aggregation keys for optimization")
    void testCommonAggregationKeyReuse() {
        List<Order> orders = createLargeOrderDataset(1000);

        orderDataSource = new InMemoryDataSource<>("orders", Order.class, orders);
        factory.registerDataSource("orders", orderDataSource, Duration.ofSeconds(10));

        DashboardBuilder<SimpleDashboard> builder = (DashboardBuilder<SimpleDashboard>) factory
                .buildDashboard(SimpleDashboardSpecificationService.INSTANCE)
                .withName("simple-dashboard");
        builder = (DashboardBuilder<SimpleDashboard>) builder
                .target(SimpleDashboard_.totalCount)
                    .from(OrderSpecificationService.INSTANCE, pk -> pk.field(Order_.id), fk -> fk.field(Order_.id))
                    .count();
        builder = (DashboardBuilder<SimpleDashboard>) builder
                .target(SimpleDashboard_.totalAmount)
                    .from(OrderSpecificationService.INSTANCE, pk -> pk.field(Order_.id), fk -> fk.field(Order_.id))
                    .sum(sfb -> sfb.field(Order_.totalAmount));
        builder = (DashboardBuilder<SimpleDashboard>) builder
                .target(SimpleDashboard_.averageAmount)
                    .from(OrderSpecificationService.INSTANCE, pk -> pk.field(Order_.id), fk -> fk.field(Order_.id))
                    .avg(sfb -> sfb.field(Order_.totalAmount));
        dashboard = builder.build();

        long startTime = System.currentTimeMillis();
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        DataSyncTestHelper.awaitSync(() -> dashboard.getData() != null, Duration.ofSeconds(10));
        long endTime = System.currentTimeMillis();

        assertThat(endTime - startTime).isLessThan(5000);

        SimpleDashboard result = dashboard.getData();
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(1000);
        assertThat(result.getTotalAmount()).isGreaterThan(0.0);
        assertThat(result.getAverageAmount()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Should handle multiple dashboard mappings efficiently")
    void testMultipleDashboardMappings() {
        List<Order> orders = createOrderData();

        orderDataSource = new InMemoryDataSource<>("orders", Order.class, orders);
        factory.registerDataSource("orders", orderDataSource, Duration.ofSeconds(10));

        DashboardBuilder<SimpleDashboard> builderMulti = (DashboardBuilder<SimpleDashboard>) factory
                .buildDashboard(SimpleDashboardSpecificationService.INSTANCE)
                .withName("simple-dashboard");
        builderMulti = (DashboardBuilder<SimpleDashboard>) builderMulti
                .target(SimpleDashboard_.totalCount)
                    .from(OrderSpecificationService.INSTANCE, pk -> pk.field(Order_.id), fk -> fk.field(Order_.id))
                    .count();
        builderMulti = (DashboardBuilder<SimpleDashboard>) builderMulti
                .target(SimpleDashboard_.totalAmount)
                    .from(OrderSpecificationService.INSTANCE, pk -> pk.field(Order_.id), fk -> fk.field(Order_.id))
                    .sum(sfb -> sfb.field(Order_.totalAmount));
        dashboard = builderMulti.build();

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        DataSyncTestHelper.awaitSync(() -> dashboard.getData() != null, Duration.ofSeconds(5));

        SimpleDashboard result = dashboard.getData();
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(5);
        assertThat(result.getTotalAmount()).isEqualTo(1500.0);
    }

    private List<Order> createOrderData() {
        List<Order> orders = new ArrayList<>();
        orders.add(new Order(1L, 1L, 100.0));
        orders.add(new Order(2L, 1L, 200.0));
        orders.add(new Order(3L, 2L, 300.0));
        orders.add(new Order(4L, 2L, 400.0));
        orders.add(new Order(5L, 3L, 500.0));
        return orders;
    }

    private List<Order> createLargeOrderDataset(int count) {
        List<Order> orders = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            long customerId = (i % 100) + 1;
            double amount = 100.0 + (i % 500);
            orders.add(new Order(i, customerId, amount));
        }
        return orders;
    }
}
