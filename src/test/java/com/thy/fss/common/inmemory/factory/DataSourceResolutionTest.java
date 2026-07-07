package com.thy.fss.common.inmemory.factory;

import com.thy.fss.common.inmemory.datasource.DataSource;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for datasource resolution by class type.
 * Tests the requirement that datasources must be matched by class type reference,
 * not by name, and validates error handling for missing or duplicate datasources.
 */
@DisplayName("DataSource Resolution by Class Type Tests")
class DataSourceResolutionTest {

    private static final String CUSTOMERS = "customers";
    private static final String ORDERS = "orders";
    private static final String ORDERS1 = "orders1";
    private static final String ORDERS2 = "orders2";

    private InMemorySpecStoreFactory factory;
    private DataSource<Customer> customerDataSource;
    private DataSource<Order> orderDataSource;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        List<Customer> customers = createTestCustomers();
        List<Order> orders = createTestOrders();

        customerDataSource = new InMemoryDataSource<>("customer-datasource", Customer.class, customers);
        orderDataSource = new InMemoryDataSource<>("order-datasource", Order.class, orders);
    }

    @AfterEach
    void tearDown() {
        factory.clearAll();
    }

    private List<Customer> createTestCustomers() {
        List<Customer> customers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Customer customer = new Customer();
            customer.setId((long) i);
            customer.setName("Customer " + i);
            customer.setEmail("customer" + i + "@test.com");
            customer.setActive(true);
            customer.setRegistrationDate(LocalDateTime.now().minusDays(i));
            customers.add(customer);
        }
        return customers;
    }

    private List<Order> createTestOrders() {
        List<Order> orders = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            Order order = new Order();
            order.setId((long) i);
            order.setCustomerId((long) ((i % 10) + 1));
            order.setTotalAmount(100.0 * i);
            order.setStatus("COMPLETED");
            order.setOrderDate(LocalDateTime.now().minusDays(i));
            orders.add(order);
        }
        return orders;
    }

    @Test
    @DisplayName("Should resolve datasource by class type reference")
    void testResolveDatasourceByClassType() {
        factory.registerDataSource(CUSTOMERS, customerDataSource, Duration.ofMinutes(5));
        factory.registerDataSource(ORDERS, orderDataSource, Duration.ofMinutes(5));

        var builder = factory.buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class);

        var afterTarget = builder.target(Customer_.totalSpent)
                .from(OrderSpecificationService.INSTANCE,
                        pk -> pk.field(Customer_.id),
                        fk -> fk.field(Order_.customerId))
                .sum(nav -> nav.field(Order_.totalAmount));

        InMemoryDataStore<Customer> store = ((InMemoryStoreBuilder<Customer>) afterTarget).build();

        assertNotNull(store, "Store should be created successfully");
    }

    @Test
    @DisplayName("Should fail when no datasource matches the class type")
    void testNoDatasourceForClassType() {
        factory.registerDataSource(CUSTOMERS, customerDataSource, Duration.ofMinutes(5));

        var builder = factory.buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class);

        var targetBuilder = builder.target(Customer_.totalSpent);
        var fromBuilder = targetBuilder.from(OrderSpecificationService.INSTANCE,
                pk -> pk.field(Customer_.id),
                fk -> fk.field(Order_.customerId));

        // When & Then - Exception is thrown during sum() - this is the actual behavior
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> fromBuilder.sum(nav -> nav.field(Order_.totalAmount))
        );

        assertTrue(exception.getMessage().contains("No datasource found for class type"),
                "Exception should mention no datasource found");
        assertTrue(exception.getMessage().contains(Order.class.getName()),
                "Exception should mention the class type");
    }
    @Test
    @DisplayName("Should fail when multiple datasources match the same class type")
    void testMultipleDatasourcesForSameClassType() {
        DataSource<Order> orderDataSource2 = new InMemoryDataSource<>(
                "order-datasource-2", Order.class, createTestOrders());

        factory.registerDataSource(CUSTOMERS, customerDataSource, Duration.ofMinutes(5));
        factory.registerDataSource(ORDERS1, orderDataSource, Duration.ofMinutes(5));
        factory.registerDataSource(ORDERS2, orderDataSource2, Duration.ofMinutes(5));

        var builder = factory.buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class);

        // Given
        var targetBuilder = builder.target(Customer_.totalSpent);
        var fromBuilder = targetBuilder.from(OrderSpecificationService.INSTANCE,
                pk -> pk.field(Customer_.id),
                fk -> fk.field(Order_.customerId));

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> fromBuilder.sum(nav -> nav.field(Order_.totalAmount))
        );

        assertTrue(exception.getMessage().contains("Multiple datasources found for class type"),
                "Exception should mention multiple datasources");
        assertTrue(exception.getMessage().contains(Order.class.getName()),
                "Exception should mention the class type");
        assertTrue(exception.getMessage().contains(ORDERS1) && exception.getMessage().contains(ORDERS2),
                "Exception should list the matching datasource names");
    }

    @Test
    @DisplayName("Should match datasource by class reference, not by name")
    void testMatchByClassReferenceNotName() {
        factory.registerDataSource("completely-different-name", customerDataSource, Duration.ofMinutes(5));
        factory.registerDataSource("another-unrelated-name", orderDataSource, Duration.ofMinutes(5));

        var builder = factory.buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class);

        var afterTarget = builder.target(Customer_.totalSpent)
                .from(OrderSpecificationService.INSTANCE,
                        pk -> pk.field(Customer_.id),
                        fk -> fk.field(Order_.customerId))
                .sum(nav -> nav.field(Order_.totalAmount));

        InMemoryDataStore<Customer> store = ((InMemoryStoreBuilder<Customer>) afterTarget).build();

        assertNotNull(store, "Store should be created successfully even with unrelated datasource names");
    }

    @Test
    @DisplayName("Should work with multiple property mappings")
    void testMultiplePropertyMappings() {
        factory.registerDataSource(CUSTOMERS, customerDataSource, Duration.ofMinutes(5));
        factory.registerDataSource(ORDERS, orderDataSource, Duration.ofMinutes(5));

        var builder1 = factory.buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class);

        var afterTarget1 = builder1
                .target(Customer_.totalSpent)
                .from(OrderSpecificationService.INSTANCE,
                        pk -> pk.field(Customer_.id),
                        fk -> fk.field(Order_.customerId))
                .sum(nav -> nav.field(Order_.totalAmount));


        var afterTarget2 = builder1
                .target(Customer_.totalSpent)
                .from(OrderSpecificationService.INSTANCE,
                        pk -> pk.field(Customer_.id),
                        fk -> fk.field(Order_.customerId))
                .sum(nav -> nav.field(Order_.totalAmount));

        InMemoryDataStore<Customer> store = ((InMemoryStoreBuilder<Customer>) afterTarget1).build();
        InMemoryDataStore<Customer> store2 = ((InMemoryStoreBuilder<Customer>) afterTarget2).build();

        assertNotNull(store, "Store with multiple mappings should be created successfully");
        assertNotNull(store2, "Second store with multiple mappings should be created successfully");
    }


    @Test
    @DisplayName("Should validate datasource exists before building store")
    void testValidateDatasourceExistsBeforeBuild() {
        factory.registerDataSource(CUSTOMERS, customerDataSource, Duration.ofMinutes(5));

        var builder = factory.buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class);

        // Given
        var targetBuilder = builder.target(Customer_.totalSpent);
        var fromBuilder = targetBuilder.from(OrderSpecificationService.INSTANCE,
                pk -> pk.field(Customer_.id),
                fk -> fk.field(Order_.customerId));

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> fromBuilder.sum(nav -> nav.field(Order_.totalAmount))
        );

        assertTrue(exception.getMessage().contains("No datasource found"),
                "Should fail with appropriate error message");
    }

    @Test
    @DisplayName("Should handle multiple different class types correctly")
    void testMultipleDifferentClassTypes() {
        factory.registerDataSource(CUSTOMERS, customerDataSource, Duration.ofMinutes(5));
        factory.registerDataSource(ORDERS, orderDataSource, Duration.ofMinutes(5));


        var builder1 = factory.buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class);

        var afterTarget1 = builder1
                .target(Customer_.totalSpent)
                .from(OrderSpecificationService.INSTANCE,
                        pk -> pk.field(Customer_.id),
                        fk -> fk.field(Order_.customerId))
                .sum(nav -> nav.field(Order_.totalAmount));

        InMemoryDataStore<Customer> customerStore = ((InMemoryStoreBuilder<Customer>) afterTarget1).build();
        InMemoryDataStore<Order> orderStore = factory.buildInMemoryStore(OrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(Order.class)
                .build();

        assertNotNull(customerStore, "Customer store should be created");
        assertNotNull(orderStore, "Order store should be created");
    }
}
