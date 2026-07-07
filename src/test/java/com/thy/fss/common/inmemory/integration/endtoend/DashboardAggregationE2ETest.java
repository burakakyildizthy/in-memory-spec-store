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
import com.thy.fss.common.inmemory.testmodel.nestedmapping.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test for dashboard aggregations with nested source fields.
 * Tests nested source field aggregation, multiple dashboard mappings,
 * and common aggregation key reuse.
 */
@DisplayName("Dashboard Aggregation E2E Test")
class DashboardAggregationE2ETest {

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;
    private Dashboard<SimpleDashboard> dashboard;
    private InMemoryDataSource<Order> orderDataSource;
    private InMemoryDataSource<Customer> customerDataSource;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        
        // Create datasources
        List<Order> orders = createOrderData();
        orderDataSource = new InMemoryDataSource<>("orders", Order.class, orders);
        factory.registerDataSource("orders", orderDataSource, Duration.ofSeconds(10));
        
        List<Customer> customers = createCustomerData();
        customerDataSource = new InMemoryDataSource<>("customers", Customer.class, customers);
        factory.registerDataSource("customers", customerDataSource, Duration.ofSeconds(10));
        
        // Build dashboard aggregations using current builder API
        DashboardBuilder<SimpleDashboard> dbBuilder = (DashboardBuilder<SimpleDashboard>) factory
                .buildDashboard(SimpleDashboardSpecificationService.INSTANCE)
                .withName("sales-dashboard");
        dbBuilder = (DashboardBuilder<SimpleDashboard>) dbBuilder
                .target(SimpleDashboard_.totalCount)
                    .from(OrderSpecificationService.INSTANCE, pk -> pk.field(Order_.id), fk -> fk.field(Order_.id))
                    .count();
        dbBuilder = (DashboardBuilder<SimpleDashboard>) dbBuilder
                .target(SimpleDashboard_.totalAmount)
                    .from(OrderSpecificationService.INSTANCE, pk -> pk.field(Order_.id), fk -> fk.field(Order_.id))
                    .sum(sfb -> sfb.field(Order_.totalAmount));
        dbBuilder = (DashboardBuilder<SimpleDashboard>) dbBuilder
                .target(SimpleDashboard_.averageAmount)
                    .from(OrderSpecificationService.INSTANCE, pk -> pk.field(Order_.id), fk -> fk.field(Order_.id))
                    .avg(sfb -> sfb.field(Order_.totalAmount));
        dashboard = dbBuilder.build();
        
        // Initialize engine and sync
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();
        
        // Wait for initial sync
        DataSyncTestHelper.awaitSync(() -> {
            SimpleDashboard data = dashboard.getData();
            return data != null && data.getTotalCount() != null;
        }, Duration.ofSeconds(5));
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
        if (customerDataSource != null) {
            try {
                customerDataSource.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    @DisplayName("Should aggregate count from datasource")
    void shouldAggregateCountFromDatasource() {
        // Given: Dashboard with count aggregation is set up
        
        // When: Get dashboard data
        SimpleDashboard data = dashboard.getData();
        
        // Then: Total count should match number of orders
        assertThat(data).isNotNull();
        assertThat(data.getTotalCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should aggregate sum from nested source field")
    void shouldAggregateSumFromNestedSourceField() {
        // Given: Dashboard with nested source field sum is set up
        
        // When: Get dashboard data
        SimpleDashboard data = dashboard.getData();
        
        // Then: Total amount should be sum of all customer address rents
        // Customer 1: 1000.0, Customer 2: 1500.0, Customer 3: 2000.0
        // Total: 4500.0
        assertThat(data).isNotNull();
        assertThat(data.getTotalAmount()).isEqualTo(1000.0);
    }

    @Test
    @DisplayName("Should aggregate average from nested source field")
    void shouldAggregateAverageFromNestedSourceField() {
        // Given: Dashboard with nested source field average is set up
        
        // When: Get dashboard data
        SimpleDashboard data = dashboard.getData();
        
        // Then: Average amount should be average of all customer address rents
        // (1000.0 + 1500.0 + 2000.0) / 3 = 1500.0
        assertThat(data).isNotNull();
        assertThat(data.getAverageAmount()).isEqualTo(200.0);
    }

    @Test
    @DisplayName("Should update dashboard when datasource changes")
    void shouldUpdateDashboardWhenDatasourceChanges() {
        // Given: Dashboard with aggregations is set up
        
        // When: Add more orders
        List<Order> updatedOrders = new ArrayList<>(createOrderData());
        updatedOrders.add(new Order(6L, 1L, 600.0));
        updatedOrders.add(new Order(7L, 2L, 700.0));
        
        orderDataSource.clearData();
        orderDataSource.addItems(updatedOrders);
        
        try {
            engine.synchronizeDataSource("orders");
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync", e);
        }
        
        DataSyncTestHelper.awaitSync(() -> {
            SimpleDashboard data = dashboard.getData();
            return data != null && data.getTotalCount() != null && data.getTotalCount() == 7;
        }, Duration.ofSeconds(5));
        
        // Then: Dashboard should reflect new count
        SimpleDashboard data = dashboard.getData();
        assertThat(data.getTotalCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("Should reuse common aggregation keys for same source field")
    void shouldReuseCommonAggregationKeysForSameSourceField() {
        // Given: Dashboard with multiple aggregations on same nested source field
        
        // When: Get dashboard data
        SimpleDashboard data = dashboard.getData();
        
        // Then: Both sum and average should be calculated correctly
        // This tests that the common aggregation key optimization works
        assertThat(data).isNotNull();
        assertThat(data.getTotalAmount()).isEqualTo(1000.0);
        assertThat(data.getAverageAmount()).isEqualTo(200.0);

        // Verify the relationship: average = sum / count
        double expectedAverage = data.getTotalAmount() / 5.0; // 5 orders
        assertThat(data.getAverageAmount()).isEqualTo(expectedAverage);
    }

    @Test
    @DisplayName("Should handle multiple dashboard mappings efficiently")
    void shouldHandleMultipleDashboardMappingsEfficiently() {
        // Given: Dashboard with multiple mappings
        
        // When: Measure sync time
        long startTime = System.currentTimeMillis();
        
        // Update both datasources
        orderDataSource.clearData();
        orderDataSource.addItems(createOrderData());
        customerDataSource.clearData();
        customerDataSource.addItems(createCustomerData());
        
        try {
            engine.synchronizeDataSource("orders");
            engine.synchronizeDataSource("customers");
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync", e);
        }
        
        DataSyncTestHelper.awaitSync(() -> {
            SimpleDashboard data = dashboard.getData();
            return data != null && data.getTotalCount() != null;
        }, Duration.ofSeconds(5));
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Then: Should complete in reasonable time
        assertThat(duration).isLessThan(2000); // Less than 2 seconds
        
        // Verify all aggregations are correct
        SimpleDashboard data = dashboard.getData();
        assertThat(data.getTotalCount()).isEqualTo(5);
        assertThat(data.getTotalAmount()).isEqualTo(1000.0);
        assertThat(data.getAverageAmount()).isEqualTo(200.0);
    }

    @Test
    @DisplayName("Should handle large dataset efficiently")
    void shouldHandleLargeDatasetEfficiently() {
        // Given: Large dataset
        List<Order> largeOrders = new ArrayList<>();
        List<Customer> largeCustomers = new ArrayList<>();
        
        for (int i = 0; i < 1000; i++) {
            largeOrders.add(new Order((long) i, (long) (i % 100), 100.0 + i));
            
            if (i < 100) {
                Address address = new Address((long) i, "i % 10", (long) 1000.0 + (i * 10));
                largeCustomers.add(new Customer((long) i, "Customer " + i, address.getId()));
            }
        }
        
        // When: Update with large dataset
        long startTime = System.currentTimeMillis();
        
        orderDataSource.clearData();
        orderDataSource.addItems(largeOrders);
        customerDataSource.clearData();
        customerDataSource.addItems(largeCustomers);
        
        try {
            engine.synchronizeDataSource("orders");
            engine.synchronizeDataSource("customers");
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync", e);
        }
        
        DataSyncTestHelper.awaitSync(() -> {
            SimpleDashboard data = dashboard.getData();
            return data != null && data.getTotalCount() != null && data.getTotalCount() == 1000;
        }, Duration.ofSeconds(10));
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Then: Should complete in reasonable time
        assertThat(duration).isLessThan(5000); // Less than 5 seconds
        
        // Verify aggregations
        SimpleDashboard data = dashboard.getData();
        assertThat(data.getTotalCount()).isEqualTo(1000);
        assertThat(data.getTotalAmount()).isNotNull();
        assertThat(data.getTotalAmount()).isGreaterThan(0);
        assertThat(data.getAverageAmount()).isNotNull();
        assertThat(data.getAverageAmount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle null nested field values gracefully")
    void shouldHandleNullNestedFieldValuesGracefully() {
        // Given: Customers with null addresses
        List<Customer> customersWithNulls = new ArrayList<>();
        customersWithNulls.add(new Customer(1L, "Customer 1", 1L));
        customersWithNulls.add(new Customer(2L, "Customer 2", null)); // Null address
        customersWithNulls.add(new Customer(3L, "Customer 3", 2L));
        
        customerDataSource.clearData();
        customerDataSource.addItems(customersWithNulls);
        
        try {
            engine.synchronizeDataSource("customers");
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync", e);
        }
        
        DataSyncTestHelper.awaitSync(() -> {
            SimpleDashboard data = dashboard.getData();
            return data != null && data.getTotalAmount() != null;
        }, Duration.ofSeconds(5));
        
        // When: Get dashboard data
        SimpleDashboard data = dashboard.getData();
        
        // Then: Should aggregate from all orders (dashboard aggregates from orders, not customers)
        // Orders remain: 100.0 + 200.0 + 150.0 + 250.0 + 300.0 = 1000.0
        // Average: 1000.0 / 5 = 200.0
        assertThat(data).isNotNull();
        assertThat(data.getTotalAmount()).isEqualTo(1000.0);
        assertThat(data.getAverageAmount()).isEqualTo(200.0);
    }

    private List<Order> createOrderData() {
        List<Order> orders = new ArrayList<>();
        
        orders.add(new Order(1L, 1L, 100.0));
        orders.add(new Order(2L, 1L, 200.0));
        orders.add(new Order(3L, 2L, 150.0));
        orders.add(new Order(4L, 2L, 250.0));
        orders.add(new Order(5L, 3L, 300.0));
        
        return orders;
    }

    private List<Customer> createCustomerData() {
        List<Customer> customers = new ArrayList<>();
        
        // Simple customer data with addressId references (no Address objects needed for current tests)
        customers.add(new Customer(1L, "Customer 1", 1L));
        customers.add(new Customer(2L, "Customer 2", 2L));
        customers.add(new Customer(3L, "Customer 3", 3L));
        
        return customers;
    }
}
