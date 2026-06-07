package com.thy.fss.common.inmemory.factory;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.Customer;
import com.thy.fss.common.inmemory.testmodel.CustomerSpecificationService;
import com.thy.fss.common.inmemory.testmodel.Order;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test coverage for InMemoryStoreBuilder with new API.
 * Tests fluent API, validation, build process, and configuration options.
 */
class InMemoryStoreBuilderTest {

    private InMemorySpecStoreFactory factory;
    private InMemoryDataSource<Customer> customerDataSource;
    private InMemoryDataSource<Order> orderDataSource;
    private List<Customer> testCustomers;
    private List<Order> testOrders;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        
        LargeDatasetGenerator generator = new LargeDatasetGenerator();
        testCustomers = generator.generateCustomers(10_000);
        testOrders = generator.generateOrders(10_000, 5);
        
        customerDataSource = new InMemoryDataSource<>("customers", Customer.class, testCustomers);
        orderDataSource = new InMemoryDataSource<>("orders", Order.class, testOrders);
        
        factory.registerDataSource(Customer.class, customerDataSource, Duration.ofSeconds(10));
        factory.registerDataSource(Order.class, orderDataSource, Duration.ofSeconds(10));
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
    }

    @Test
    void testFluentAPIBasicStoreCreation() {
        InMemoryDataStore<Customer> store = factory
                .buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class)
                .build();

        assertThat(store).isNotNull();
        assertThat(store.getTargetClass()).isEqualTo(Customer.class);
        assertThat(store.getStoreId()).isNotNull().startsWith("store-");
    }

    @Test
    void testValidationNullEntityClass() {
        assertThatThrownBy(() -> new InMemoryStoreBuilder<>(factory, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Target service cannot be null");
    }

    @Test
    void testValidationNullFactory() {
        assertThatThrownBy(() -> new InMemoryStoreBuilder<>(null, CustomerSpecificationService.INSTANCE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Factory cannot be null");
    }

    @Test
    void testValidationNullDataSourceClass() {
        InMemoryStoreBuilder<Customer> builder = factory.buildInMemoryStore(CustomerSpecificationService.INSTANCE);

        assertThatThrownBy(() -> builder.withPrimaryDataSource(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DataSource class cannot be null");
    }

    @Test
    void testValidationBuildWithoutPrimaryDataSource() {
        InMemoryStoreBuilder<Customer> builder = factory.buildInMemoryStore(CustomerSpecificationService.INSTANCE);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Primary datasource must be set");
    }

    @Test
    void testValidationNonExistentDataSource() {
        InMemoryStoreBuilder<Customer> builder = factory
                .buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(String.class);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testBuildProcessStoreRegistration() {
        InMemoryDataStore<Customer> store = factory
                .buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class)
                .build();

        assertThat(store).isNotNull();
        assertThat(store.getStoreId()).isNotNull();
    }

    @Test
    void testBuildProcessUniqueStoreIds() {
        InMemoryDataStore<Customer> store1 = factory
                .buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class)
                .build();

        InMemoryDataStore<Customer> store2 = factory
                .buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class)
                .build();

        assertThat(store1.getStoreId()).isNotEqualTo(store2.getStoreId());
    }

    @Test
    void testConfigurationOptionsMinimalConfiguration() {
        InMemoryDataStore<Customer> store = factory
                .buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class)
                .build();

        assertThat(store).isNotNull();
        assertThat(store.getTargetClass()).isEqualTo(Customer.class);
        assertThat(store.getRootSpecification()).isNull();
        assertThat(store.getPropertyMappings()).isEmpty();
    }

    @Test
    void testConfigurationOptionsLargeDataset() {
        InMemoryDataStore<Customer> store = factory
                .buildInMemoryStore(CustomerSpecificationService.INSTANCE)
                .withPrimaryDataSource(Customer.class)
                .build();

        assertThat(store).isNotNull();
        assertThat(testCustomers).hasSize(10_000);
        assertThat(testOrders).hasSizeGreaterThanOrEqualTo(10_000);
    }
}
