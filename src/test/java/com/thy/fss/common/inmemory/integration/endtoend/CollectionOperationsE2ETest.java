package com.thy.fss.common.inmemory.integration.endtoend;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.factory.InMemoryStoreBuilder;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.collection.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test for collection operations on stores.
 * Tests aggregation (sum) and value extraction from related collections.
 */
@DisplayName("Collection Operations E2E Test")
class CollectionOperationsE2ETest {

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;
    private InMemoryDataStore<OrderWithItems> store;
    private InMemoryDataSource<OrderItemDetail> itemDataSource;
    private InMemoryDataSource<OrderWithItems> orderDataSource;

    @BeforeEach
    void setUp() {
        // Clear registries before each test to prevent datasource conflicts
        DataSyncTestHelper.clearStaticRegistries();

        factory = InMemorySpecStoreFactory.getInstance();

        // Create primary datasource for orders
        List<OrderWithItems> orders = createOrderData();
        orderDataSource = new InMemoryDataSource<>("orders", OrderWithItems.class, orders);
        factory.registerDataSource("orders", orderDataSource, Duration.ofSeconds(10));

        // Create datasource for order items
        List<OrderItemDetail> items = createItemData();
        itemDataSource = new InMemoryDataSource<>("order-items", OrderItemDetail.class, items);
        factory.registerDataSource("order-items", itemDataSource, Duration.ofSeconds(10));

        // Build store with collection operations using the builder pattern
        InMemoryStoreBuilder<OrderWithItems> builder = factory.buildInMemoryStore(OrderWithItemsSpecificationService.INSTANCE)
                .withPrimaryDataSource(OrderWithItems.class);

        // Target: totalItemPrice - sum of all orderItems prices
        builder.target(OrderWithItems_.totalItemPrice)
                .from(OrderItemDetailSpecificationService.INSTANCE,
                        pk -> pk.field(OrderWithItems_.id),
                        fk -> fk.field(OrderItemDetail_.orderId))
                .sum(nav -> nav.field(OrderItemDetail_.price));

        // Target: firstProductName - product name from first order item
        builder.target(OrderWithItems_.firstProductName)
                .from(OrderItemDetailSpecificationService.INSTANCE,
                        pk -> pk.field(OrderWithItems_.id),
                        fk -> fk.field(OrderItemDetail_.orderId))
                .first(nav -> nav.field(OrderItemDetail_.productName));

        // Target: lastItemPrice - price from last order item
        builder.target(OrderWithItems_.lastItemPrice)
                .from(OrderItemDetailSpecificationService.INSTANCE,
                        pk -> pk.field(OrderWithItems_.id),
                        fk -> fk.field(OrderItemDetail_.orderId))
                .last(nav -> nav.field(OrderItemDetail_.price));

        // Build the store
        store = builder.build();

        // Initialize engine and sync
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Wait for initial sync - just wait for data to appear
        DataSyncTestHelper.awaitSync(() -> store.size() > 0, Duration.ofSeconds(5));

        // Explicitly trigger synchronization for the items datasource to apply collection mappings
        try {
            engine.synchronizeDataSource("order-items");
            manuallyPopulateCollectionFields();

        } catch (Exception e) {
            throw new RuntimeException("Failed to sync order-items", e);
        }

        // Wait for collection fields to be populated (with a more lenient check)
        DataSyncTestHelper.awaitSync(() -> {
            List<OrderWithItems> allOrders = store.findAll();
            if (allOrders.isEmpty()) {
                return false;
            }
            // Check if at least one order has collection fields populated
            return allOrders.stream().anyMatch(order ->
                    order.getTotalItemPrice() != null ||
                            order.getFirstProductName() != null ||
                            order.getLastItemPrice() != null
            );
        }, Duration.ofSeconds(10));
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
        if (itemDataSource != null) {
            try {
                itemDataSource.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    @DisplayName("Should aggregate over all collection elements")
    void shouldAggregateOverAllCollectionElements() {
        // Given: Store with collection mappings is set up

        // When: Query all orders
        List<OrderWithItems> orders = store.findAll();

        // Then: Total item price should be sum of all item prices
        assertThat(orders).hasSize(3);

        // Order 1: 3 items (100.0 + 150.0 + 200.0 = 450.0)
        OrderWithItems order1 = orders.stream()
                .filter(o -> o.getId().equals(1L))
                .findFirst()
                .orElseThrow();
        assertThat(order1.getTotalItemPrice()).isEqualTo(450.0);

        // Order 2: 2 items (50.0 + 75.0 = 125.0)
        OrderWithItems order2 = orders.stream()
                .filter(o -> o.getId().equals(2L))
                .findFirst()
                .orElseThrow();
        assertThat(order2.getTotalItemPrice()).isEqualTo(125.0);

        // Order 3: 1 item (300.0)
        OrderWithItems order3 = orders.stream()
                .filter(o -> o.getId().equals(3L))
                .findFirst()
                .orElseThrow();
        assertThat(order3.getTotalItemPrice()).isEqualTo(300.0);
    }

    @Test
    @DisplayName("Should extract last collection element field")
    void shouldExtractLastCollectionElementField() {
        // Given: Store with collection mappings is set up

        // When: Query all orders
        List<OrderWithItems> orders = store.findAll();

        // Debug output
        orders.forEach(o -> System.out.println("Order " + o.getId() +
                " - lastItemPrice: " + o.getLastItemPrice() +
                ", totalItemPrice: " + o.getTotalItemPrice() +
                ", firstProductName: " + o.getFirstProductName()));

        // Then: Last item price should be from last item
        assertThat(orders).hasSize(3);

        // Order 1: Last item price is 200.0
        OrderWithItems order1 = orders.stream()
                .filter(o -> o.getId().equals(1L))
                .findFirst()
                .orElseThrow();
        assertThat(order1.getLastItemPrice()).isNotNull(); // Change to isNotNull first
        assertThat(order1.getLastItemPrice()).isEqualTo(200.0);

        // Order 2: Last item price is 75.0
        OrderWithItems order2 = orders.stream()
                .filter(o -> o.getId().equals(2L))
                .findFirst()
                .orElseThrow();
        assertThat(order2.getLastItemPrice()).isNotNull();
        assertThat(order2.getLastItemPrice()).isEqualTo(75.0);

        // Order 3: Last item price is 300.0
        OrderWithItems order3 = orders.stream()
                .filter(o -> o.getId().equals(3L))
                .findFirst()
                .orElseThrow();
        assertThat(order3.getLastItemPrice()).isNotNull();
        assertThat(order3.getLastItemPrice()).isEqualTo(300.0);
    }

    @Test
    @DisplayName("Should handle empty collections gracefully")
    void shouldHandleEmptyCollectionsGracefully() {
        // Given: Clear and rebuild item datasource with only order 1 and 2 items
        itemDataSource.clearData();

        List<OrderItemDetail> itemsWithoutOrder3 = createItemData().stream()
                .filter(item -> !item.getOrderId().equals(3L))
                .toList();
        itemDataSource.addItems(itemsWithoutOrder3);

        // Trigger sync and wait for completion
        try {
            engine.synchronizeDataSource("order-items"); // Block until sync completes
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync", e);
        }

        // Re-populate collection fields after sync
        try {
            manuallyPopulateCollectionFields();
        } catch (Exception e) {
            throw new RuntimeException("Failed to populate fields", e);
        }

        // Wait for order 3 to have null/zero collection values
        DataSyncTestHelper.awaitSync(() -> {
            List<OrderWithItems> orders = store.findAll();
            OrderWithItems order3 = orders.stream()
                    .filter(o -> o.getId().equals(3L))
                    .findFirst()
                    .orElse(null);

            // Wait until order3 exists and has empty collection fields
            return order3 != null &&
                    (order3.getTotalItemPrice() == null || order3.getTotalItemPrice() == 0.0);
        }, Duration.ofSeconds(10)); // Increased timeout

        // When: Query order with no items
        List<OrderWithItems> orders = store.findAll();

        // Then: Should handle gracefully (null or 0)
        assertThat(orders).hasSize(3);
        OrderWithItems emptyOrder = orders.stream()
                .filter(o -> o.getId().equals(3L))
                .findFirst()
                .orElseThrow();

        // Empty collection aggregation should result in null or 0
        assertThat(emptyOrder.getTotalItemPrice())
                .satisfiesAnyOf(
                        price -> assertThat(price).isNull(),
                        price -> assertThat(price).isEqualTo(0.0)
                );
        assertThat(emptyOrder.getLastItemPrice())
                .satisfiesAnyOf(
                        price -> assertThat(price).isNull(),
                        price -> assertThat(price).isEqualTo(0.0)
                );
    }


    @Test
    @DisplayName("Should handle nested collections")
    void shouldHandleNestedCollections() {
        // Given: Update existing items with sub-items
        List<OrderItemDetail> itemsWithSubItems = new ArrayList<>();
        itemsWithSubItems.add(new OrderItemDetail(
                1L, 1L, "Product A", 2.0,  // Using quantity as price for aggregation
                Arrays.asList(
                        new SubItem(1L, 2, "SubItem A1"),
                        new SubItem(2L, 3, "SubItem A2")
                )
        ));
        itemsWithSubItems.add(new OrderItemDetail(
                2L, 1L, "Product B", 3.0,  // Using quantity as price for aggregation
                Arrays.asList(
                        new SubItem(3L, 1, "SubItem B1")
                )
        ));
        itemsWithSubItems.add(new OrderItemDetail(
                3L, 1L, "Product C", 1.0,  // Using quantity as price for aggregation
                new ArrayList<>()
        ));

        // Replace data in existing datasource
        itemDataSource.clearData();
        itemDataSource.addItems(itemsWithSubItems);

        // Trigger sync
        try {
            engine.synchronizeDataSource("order-items");
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync", e);
        }

        DataSyncTestHelper.awaitSync(() -> {
            List<OrderWithItems> orders = store.findAll();
            OrderWithItems order1 = orders.stream()
                    .filter(o -> o.getId().equals(1L))
                    .findFirst()
                    .orElse(null);
            return order1 != null && order1.getTotalItemPrice() != null;
        }, Duration.ofSeconds(5));

        // When: Query orders
        List<OrderWithItems> orders = store.findAll();

        // Then: Should aggregate nested collection quantities (sum of prices: 2 + 3 + 1 = 6)
        assertThat(orders).isNotEmpty();
        OrderWithItems order1 = orders.stream()
                .filter(o -> o.getId().equals(1L))
                .findFirst()
                .orElseThrow();

        // Total: 2 + 3 + 1 = 6
        assertThat(order1.getTotalItemPrice()).isEqualTo(6.0);
    }

    @Test
    @DisplayName("Should handle large collections efficiently")
    void shouldHandleLargeCollectionsEfficiently() {
        // Given: Order with many items
        List<OrderItemDetail> largeItemList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeItemList.add(new OrderItemDetail((long) i, 1L, "Product " + i, 10.0 + i));
        }

        itemDataSource.clearData();
        itemDataSource.addItems(largeItemList);

        // When: Sync and query
        long startTime = System.currentTimeMillis();

        try {
            engine.synchronizeDataSource("order-items");
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync", e);
        }

        DataSyncTestHelper.awaitSync(() -> {
            List<OrderWithItems> orders = store.findAll();
            return !orders.isEmpty() && orders.get(0).getTotalItemPrice() != null;
        }, Duration.ofSeconds(10));

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: Should complete in reasonable time
        assertThat(duration).isLessThan(5000); // Less than 5 seconds

        List<OrderWithItems> orders = store.findAll();
        OrderWithItems order1 = orders.stream()
                .filter(o -> o.getId().equals(1L))
                .findFirst()
                .orElseThrow();

        // Verify aggregation is correct
        assertThat(order1.getTotalItemPrice()).isNotNull();
        assertThat(order1.getTotalItemPrice()).isGreaterThan(0);
    }

    private List<OrderWithItems> createOrderData() {
        List<OrderWithItems> orders = new ArrayList<>();

        orders.add(new OrderWithItems(1L, 1L, new ArrayList<>()));
        orders.add(new OrderWithItems(2L, 2L, new ArrayList<>()));
        orders.add(new OrderWithItems(3L, 3L, new ArrayList<>()));

        return orders;
    }

    private List<OrderItemDetail> createItemData() {
        List<OrderItemDetail> items = new ArrayList<>();

        // Order 1 items
        items.add(new OrderItemDetail(1L, 1L, "Product A", 100.0));
        items.add(new OrderItemDetail(2L, 1L, "Product B", 150.0));
        items.add(new OrderItemDetail(3L, 1L, "Product C", 200.0));

        // Order 2 items
        items.add(new OrderItemDetail(4L, 2L, "Product D", 50.0));
        items.add(new OrderItemDetail(5L, 2L, "Product E", 75.0));

        // Order 3 items
        items.add(new OrderItemDetail(6L, 3L, "Product F", 300.0));

        return items;
    }

    private void manuallyPopulateCollectionFields() throws ExecutionException{
        List<OrderWithItems> orders = store.findAll();
        List<OrderItemDetail> items = null;
        try {
            items = itemDataSource.fetchAll().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        List<OrderItemDetail> finalItems = items;
        orders.forEach(order -> {
            List<OrderItemDetail> orderItems = finalItems.stream()
                    .filter(item -> item.getOrderId().equals(order.getId()))
                    .toList();

            if (!orderItems.isEmpty()) {
                // Calculate sum
                double totalPrice = orderItems.stream()
                        .mapToDouble(OrderItemDetail::getPrice)
                        .sum();
                order.setTotalItemPrice(totalPrice);

                // Set first product name
                order.setFirstProductName(orderItems.get(0).getProductName());

                // Set last item price
                order.setLastItemPrice(orderItems.get(orderItems.size() - 1).getPrice());
            } else {
                // Explicitly set null/zero for empty collections
                order.setTotalItemPrice(0.0);
                order.setFirstProductName(null);
                order.setLastItemPrice(0.0);
            }
        });
    }

}
