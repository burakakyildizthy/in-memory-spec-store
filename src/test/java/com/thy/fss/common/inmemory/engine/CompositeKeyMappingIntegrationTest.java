package com.thy.fss.common.inmemory.engine;

import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.factory.InMemoryStoreBuilder;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.*;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Order;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for composite key mapping functionality.
 * 
 * These tests verify end-to-end behavior of composite key mappings
 * including matching, aggregation, and filtering.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CompositeKeyMappingIntegrationTest {

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine syncEngine;
    private InMemoryDataSource<TestTarget> targetDataSource;
    private InMemoryDataSource<TestSource> sourceDataSource;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();
    }

    @AfterEach
    void tearDown() {
        if (syncEngine != null) {
            syncEngine.close();
        }
        if (targetDataSource != null) {
            targetDataSource.close();
        }
        if (sourceDataSource != null) {
            sourceDataSource.close();
        }
        factory.clearAll();
    }

    /**
     * Helper method to wait for synchronization
     */
    private void waitForSync() {
        TestUtil.await(1500);
    }

    /**
     * Test 15.1: Simple two-field composite key
     * 
     * Requirements: 1.3, 3.1
     * 
     * Create test entities with two-field composite keys (e.g., userId + regionId)
     * Define mapping using keys.on().on()
     * Verify correct matching and aggregation
     */
    @Test
    @Order(1)
    void testSimpleTwoFieldCompositeKey() {
        // Given: Target entities with id and code fields
        List<TestTarget> targets = new ArrayList<>();
        TestTarget target1 = new TestTarget();
        target1.setId(1L);
        target1.setCode("A");
        target1.setVersion(0);
        targets.add(target1);

        TestTarget target2 = new TestTarget();
        target2.setId(1L);
        target2.setCode("B");
        target2.setVersion(0);
        targets.add(target2);

        TestTarget target3 = new TestTarget();
        target3.setId(2L);
        target3.setCode("A");
        target3.setVersion(0);
        targets.add(target3);

        // Given: Source entities with matching composite keys and values to aggregate
        List<TestSource> sources = new ArrayList<>();
        
        // Sources matching target1 (id=1, code=A)
        TestSource source1 = new TestSource();
        source1.setTargetId(1L);
        source1.setTargetCode("A");
        source1.setTargetVersion(10);
        sources.add(source1);

        TestSource source2 = new TestSource();
        source2.setTargetId(1L);
        source2.setTargetCode("A");
        source2.setTargetVersion(20);
        sources.add(source2);

        // Source matching target2 (id=1, code=B)
        TestSource source3 = new TestSource();
        source3.setTargetId(1L);
        source3.setTargetCode("B");
        source3.setTargetVersion(30);
        sources.add(source3);

        // No sources matching target3 (id=2, code=A)

        // Setup data sources
        targetDataSource = new InMemoryDataSource<>("test-targets", TestTarget.class);
        targetDataSource.addItems(targets);
        
        sourceDataSource = new InMemoryDataSource<>("test-sources", TestSource.class);
        sourceDataSource.addItems(sources);

        factory.registerDataSource("test-targets", targetDataSource, Duration.ofMillis(500));
        factory.registerDataSource("test-sources", sourceDataSource, Duration.ofMillis(500));

        // When: Building store with two-field composite key mapping
        InMemoryStoreBuilder<TestTarget> builder = factory.buildInMemoryStore(TestTargetSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestTarget.class);
        
        builder.target(TestTarget_.version)
                .from(TestSourceSpecificationService.INSTANCE, keys -> keys
                    .on(pk -> pk.field(TestTarget_.id), fk -> fk.field(TestSource_.targetId))
                    .on(pk -> pk.field(TestTarget_.code), fk -> fk.field(TestSource_.targetCode))
                )
                .sum(nav -> nav.field(TestSource_.targetVersion));
        
        InMemoryDataStore<TestTarget> store = builder.build();

        syncEngine = new DataSynchronizationEngine(factory);
        syncEngine.initialize();
        
        // Wait for synchronization
        waitForSync();

        // Then: Verify aggregation results
        List<TestTarget> results = store.findAll();
        assertThat(results).hasSize(3);

        // Target1 should have sum of 10 + 20 = 30
        TestTarget result1 = results.stream()
                .filter(t -> t.getId().equals(1L) && "A".equals(t.getCode()))
                .findFirst()
                .orElseThrow();
        assertThat(result1.getVersion())
                .as("Target with id=1, code=A should have sum of matching sources (10+20)")
                .isEqualTo(30);

        // Target2 should have sum of 30
        TestTarget result2 = results.stream()
                .filter(t -> t.getId().equals(1L) && "B".equals(t.getCode()))
                .findFirst()
                .orElseThrow();
        assertThat(result2.getVersion())
                .as("Target with id=1, code=B should have sum of matching source (30)")
                .isEqualTo(30);

        // Target3 should have sum of 0 (no matching sources)
        TestTarget result3 = results.stream()
                .filter(t -> t.getId().equals(2L) && "A".equals(t.getCode()))
                .findFirst()
                .orElseThrow();
        assertThat(result3.getVersion())
                .as("Target with id=2, code=A should have sum of 0 (no matching sources)")
                .isZero();
    }

    /**
     * Test 15.2: Complex multi-field composite key
     * 
     * Requirements: 1.3
     * 
     * Create test entities with 4-field composite keys
     * Define mapping with four on() calls
     * Verify all fields must match for successful match
     */
    @Test
    @Order(2)
    void testComplexMultiFieldCompositeKey() {
        // Given: Target entities with 4 fields for composite key
        List<TestTarget> targets = new ArrayList<>();
        TestTarget target1 = new TestTarget();
        target1.setId(1L);
        target1.setCode("A");
        target1.setVersion(1);
        target1.setRegion("US");
        target1.setTimestamp(0L);
        targets.add(target1);

        TestTarget target2 = new TestTarget();
        target2.setId(1L);
        target2.setCode("A");
        target2.setVersion(1);
        target2.setRegion("EU");
        target2.setTimestamp(0L);
        targets.add(target2);

        // Given: Source entities with various match patterns
        List<TestSource> sources = new ArrayList<>();
        
        // Full match for target1 (id=1, code=A, version=1, region=US)
        TestSource source1 = new TestSource();
        source1.setTargetId(1L);
        source1.setTargetCode("A");
        source1.setTargetVersion(1);
        source1.setTargetRegion("US");
        source1.setTargetTimestamp(100L);
        sources.add(source1);

        TestSource source2 = new TestSource();
        source2.setTargetId(1L);
        source2.setTargetCode("A");
        source2.setTargetVersion(1);
        source2.setTargetRegion("US");
        source2.setTargetTimestamp(200L);
        sources.add(source2);

        // Partial match - only 3 fields match (missing region)
        TestSource source3 = new TestSource();
        source3.setTargetId(1L);
        source3.setTargetCode("A");
        source3.setTargetVersion(1);
        source3.setTargetRegion("ASIA");
        source3.setTargetTimestamp(300L);
        sources.add(source3);

        // Full match for target2 (id=1, code=A, version=1, region=EU)
        TestSource source4 = new TestSource();
        source4.setTargetId(1L);
        source4.setTargetCode("A");
        source4.setTargetVersion(1);
        source4.setTargetRegion("EU");
        source4.setTargetTimestamp(400L);
        sources.add(source4);

        // Setup data sources
        targetDataSource = new InMemoryDataSource<>("test-targets-4field", TestTarget.class);
        targetDataSource.addItems(targets);
        
        sourceDataSource = new InMemoryDataSource<>("test-sources-4field", TestSource.class);
        sourceDataSource.addItems(sources);

        System.out.println("=== Data Source Setup ===");
        System.out.println("Target count: " + targets.size());
        System.out.println("Source count: " + sources.size());
        for (TestSource s : sources) {
            System.out.println(String.format("Source: targetId=%d, targetCode=%s, targetVersion=%d, targetRegion=%s, targetTimestamp=%d",
                    s.getTargetId(), s.getTargetCode(), s.getTargetVersion(), s.getTargetRegion(), s.getTargetTimestamp()));
        }

        factory.registerDataSource("test-targets-4field", targetDataSource, Duration.ofMillis(500));
        factory.registerDataSource("test-sources-4field", sourceDataSource, Duration.ofMillis(500));

        // Debug: Check what data source name will be used
        String resolvedDataSourceName = factory.getDataSourceNameByClass(TestSource.class);
        System.out.println("Resolved data source name for TestSource.class: " + resolvedDataSourceName);

        // When: Building store with four-field composite key mapping
        InMemoryStoreBuilder<TestTarget> builder = factory.buildInMemoryStore(TestTargetSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestTarget.class);
        
        builder.target(TestTarget_.timestamp)
                .from(TestSourceSpecificationService.INSTANCE, keys -> keys
                    .on(pk -> pk.field(TestTarget_.id), fk -> fk.field(TestSource_.targetId))
                    .on(pk -> pk.field(TestTarget_.code), fk -> fk.field(TestSource_.targetCode))
                    .on(pk -> pk.field(TestTarget_.version), fk -> fk.field(TestSource_.targetVersion))
                    .on(pk -> pk.field(TestTarget_.region), fk -> fk.field(TestSource_.targetRegion))
                )
                .sum(nav -> nav.field(TestSource_.targetTimestamp));
        
        InMemoryDataStore<TestTarget> store = builder.build();

        syncEngine = new DataSynchronizationEngine(factory);
        syncEngine.initialize();
        
        // Wait longer for 4-field composite key synchronization
        TestUtil.await(5000);

        // Then: Verify only full matches are aggregated
        List<TestTarget> results = store.findAll();
        assertThat(results).hasSize(2);

        // Debug: Print all results
        System.out.println("=== Test Results ===");
        for (TestTarget t : results) {
            System.out.println(String.format("Target: id=%d, code=%s, version=%d, region=%s, timestamp=%d",
                    t.getId(), t.getCode(), t.getVersion(), t.getRegion(), t.getTimestamp()));
        }
        
        // Debug: Check if results are same instances as original targets
        System.out.println("Are results same instances as original targets?");
        for (TestTarget t : results) {
            boolean isSame = targets.stream().anyMatch(orig -> orig == t);
            System.out.println(String.format("Target id=%d: %s", t.getId(), isSame ? "SAME INSTANCE" : "DIFFERENT INSTANCE"));
        }

        // Target1 should have sum of 100 + 200 = 300 (only full matches)
        TestTarget result1 = results.stream()
                .filter(t -> t.getId().equals(1L) && "A".equals(t.getCode()) && 
                            t.getVersion().equals(1) && "US".equals(t.getRegion()))
                .findFirst()
                .orElseThrow();
        assertThat(result1.getTimestamp())
                .as("Target with all 4 fields matching should have sum of full matches only (100+200)")
                .isEqualTo(300L);

        // Target2 should have sum of 400
        TestTarget result2 = results.stream()
                .filter(t -> t.getId().equals(1L) && "A".equals(t.getCode()) && 
                            t.getVersion().equals(1) && "EU".equals(t.getRegion()))
                .findFirst()
                .orElseThrow();
        assertThat(result2.getTimestamp())
                .as("Target with all 4 fields matching should have sum of full match (400)")
                .isEqualTo(400L);
    }

    /**
     * Test 15.3: Composite key with nested paths
     * 
     * Requirements: 1.3
     * 
     * Create entities where key fields are nested (e.g., order.customer.id)
     * Define mapping with nested field navigation
     * Verify navigation and matching work correctly
     */
    @Test
    @Order(3)
    void testCompositeKeyWithNestedPaths() {
        // Given: Order entities with nested customer objects
        List<com.thy.fss.common.inmemory.factory.navigation.testmodel.Order> orders = new ArrayList<>();
        
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Order order1 = 
            new com.thy.fss.common.inmemory.factory.navigation.testmodel.Order();
        order1.setOrderId(1L);
        order1.setOrderCode("ORD-001");
        order1.setTotalAmount(0);
        
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer customer1 = 
            new com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer();
        customer1.setCustomerId(100L);
        customer1.setCustomerName("John Doe");
        customer1.setRegion("US");
        order1.setCustomer(customer1);
        orders.add(order1);
        
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Order order2 = 
            new com.thy.fss.common.inmemory.factory.navigation.testmodel.Order();
        order2.setOrderId(2L);
        order2.setOrderCode("ORD-002");
        order2.setTotalAmount(0);
        
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer customer2 = 
            new com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer();
        customer2.setCustomerId(200L);
        customer2.setCustomerName("Jane Smith");
        customer2.setRegion("EU");
        order2.setCustomer(customer2);
        orders.add(order2);
        
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Order order3 = 
            new com.thy.fss.common.inmemory.factory.navigation.testmodel.Order();
        order3.setOrderId(3L);
        order3.setOrderCode("ORD-003");
        order3.setTotalAmount(0);
        
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer customer3 = 
            new com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer();
        customer3.setCustomerId(100L);
        customer3.setCustomerName("John Doe");
        customer3.setRegion("ASIA");  // Different region, same customer ID
        order3.setCustomer(customer3);
        orders.add(order3);

        // Given: Payment entities with nested customer objects
        List<com.thy.fss.common.inmemory.factory.navigation.testmodel.Payment> payments = new ArrayList<>();
        
        // Payments matching order1 (customerId=100, region=US)
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Payment payment1 = 
            new com.thy.fss.common.inmemory.factory.navigation.testmodel.Payment();
        payment1.setPaymentId(1L);
        payment1.setPaymentCode("PAY-001");
        payment1.setAmount(150);
        
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer paymentCustomer1 = 
            new com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer();
        paymentCustomer1.setCustomerId(100L);
        paymentCustomer1.setRegion("US");
        payment1.setCustomer(paymentCustomer1);
        payments.add(payment1);
        
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Payment payment2 = 
            new com.thy.fss.common.inmemory.factory.navigation.testmodel.Payment();
        payment2.setPaymentId(2L);
        payment2.setPaymentCode("PAY-002");
        payment2.setAmount(250);
        
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer paymentCustomer2 = 
            new com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer();
        paymentCustomer2.setCustomerId(100L);
        paymentCustomer2.setRegion("US");
        payment2.setCustomer(paymentCustomer2);
        payments.add(payment2);
        
        // Payment matching order2 (customerId=200, region=EU)
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Payment payment3 = 
            new com.thy.fss.common.inmemory.factory.navigation.testmodel.Payment();
        payment3.setPaymentId(3L);
        payment3.setPaymentCode("PAY-003");
        payment3.setAmount(300);
        
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer paymentCustomer3 = 
            new com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer();
        paymentCustomer3.setCustomerId(200L);
        paymentCustomer3.setRegion("EU");
        payment3.setCustomer(paymentCustomer3);
        payments.add(payment3);
        
        // Payment matching order3 (customerId=100, region=ASIA)
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Payment payment4 = 
            new com.thy.fss.common.inmemory.factory.navigation.testmodel.Payment();
        payment4.setPaymentId(4L);
        payment4.setPaymentCode("PAY-004");
        payment4.setAmount(400);
        
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer paymentCustomer4 = 
            new com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer();
        paymentCustomer4.setCustomerId(100L);
        paymentCustomer4.setRegion("ASIA");
        payment4.setCustomer(paymentCustomer4);
        payments.add(payment4);

        // Setup data sources
        InMemoryDataSource<com.thy.fss.common.inmemory.factory.navigation.testmodel.Order> orderDataSource = 
            new InMemoryDataSource<>("test-orders-nested", com.thy.fss.common.inmemory.factory.navigation.testmodel.Order.class);
        orderDataSource.addItems(orders);
        
        InMemoryDataSource<com.thy.fss.common.inmemory.factory.navigation.testmodel.Payment> paymentDataSource = 
            new InMemoryDataSource<>("test-payments-nested", com.thy.fss.common.inmemory.factory.navigation.testmodel.Payment.class);
        paymentDataSource.addItems(payments);

        factory.registerDataSource("test-orders-nested", orderDataSource, Duration.ofMillis(500));
        factory.registerDataSource("test-payments-nested", paymentDataSource, Duration.ofMillis(500));

        // When: Building store with composite key mapping using nested paths
        InMemoryStoreBuilder<com.thy.fss.common.inmemory.factory.navigation.testmodel.Order> builder = 
            factory.buildInMemoryStore(OrderSpecificationService.INSTANCE)
                .withPrimaryDataSource(com.thy.fss.common.inmemory.factory.navigation.testmodel.Order.class);
        
        builder.target(com.thy.fss.common.inmemory.factory.navigation.testmodel.Order_.totalAmount)
                .from(PaymentSpecificationService.INSTANCE, keys -> keys
                    // Navigate through nested customer object: order.customer.customerId -> payment.customer.customerId
                    .on(pk -> pk.field(com.thy.fss.common.inmemory.factory.navigation.testmodel.Order_.customer)
                               .field(com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer_.customerId), 
                        fk -> fk.field(com.thy.fss.common.inmemory.factory.navigation.testmodel.Payment_.customer)
                               .field(com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer_.customerId))
                    // Navigate through nested customer object: order.customer.region -> payment.customer.region
                    .on(pk -> pk.field(com.thy.fss.common.inmemory.factory.navigation.testmodel.Order_.customer)
                               .field(com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer_.region), 
                        fk -> fk.field(com.thy.fss.common.inmemory.factory.navigation.testmodel.Payment_.customer)
                               .field(com.thy.fss.common.inmemory.factory.navigation.testmodel.Customer_.region))
                )
                .sum(nav -> nav.field(com.thy.fss.common.inmemory.factory.navigation.testmodel.Payment_.amount));
        
        InMemoryDataStore<com.thy.fss.common.inmemory.factory.navigation.testmodel.Order> store = builder.build();

        syncEngine = new DataSynchronizationEngine(factory);
        syncEngine.initialize();
        
        // Wait longer for nested path synchronization
        TestUtil.await(3000);

        // Then: Verify nested path navigation and matching work correctly
        List<com.thy.fss.common.inmemory.factory.navigation.testmodel.Order> results = store.findAll();
        assertThat(results).hasSize(3);

        // Order1 should have sum of payments with customerId=100 AND region=US (150+250=400)
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Order result1 = results.stream()
                .filter(o -> o.getOrderId().equals(1L))
                .findFirst()
                .orElseThrow();
        assertThat(result1.getTotalAmount())
                .as("Order1 with nested customer.customerId=100 and customer.region=US should have sum of matching payments (150+250)")
                .isEqualTo(400);

        // Order2 should have sum of payments with customerId=200 AND region=EU (300)
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Order result2 = results.stream()
                .filter(o -> o.getOrderId().equals(2L))
                .findFirst()
                .orElseThrow();
        assertThat(result2.getTotalAmount())
                .as("Order2 with nested customer.customerId=200 and customer.region=EU should have sum of matching payment (300)")
                .isEqualTo(300);

        // Order3 should have sum of payments with customerId=100 AND region=ASIA (400)
        com.thy.fss.common.inmemory.factory.navigation.testmodel.Order result3 = results.stream()
                .filter(o -> o.getOrderId().equals(3L))
                .findFirst()
                .orElseThrow();
        assertThat(result3.getTotalAmount())
                .as("Order3 with nested customer.customerId=100 and customer.region=ASIA should have sum of matching payment (400)")
                .isEqualTo(400);
        
        // Cleanup
        orderDataSource.close();
        paymentDataSource.close();
    }

    /**
     * Test 15.4: Composite key with where clause
     * 
     * Requirements: 4.1
     * 
     * Create mapping with composite keys and where clause
     * Verify both conditions are applied correctly
     * Verify order: key match then filter
     */
    @Test
    @Order(4)
    void testCompositeKeyWithWhereClause() {
        // Given: Target entities
        List<TestTarget> targets = new ArrayList<>();
        TestTarget target1 = new TestTarget();
        target1.setId(1L);
        target1.setCode("A");
        target1.setVersion(0);
        targets.add(target1);

        // Given: Source entities with various regions
        List<TestSource> sources = new ArrayList<>();
        
        // Matches composite key AND where clause (region=US)
        TestSource source1 = new TestSource();
        source1.setTargetId(1L);
        source1.setTargetCode("A");
        source1.setTargetVersion(100);
        source1.setTargetRegion("US");
        sources.add(source1);

        TestSource source2 = new TestSource();
        source2.setTargetId(1L);
        source2.setTargetCode("A");
        source2.setTargetVersion(200);
        source2.setTargetRegion("US");
        sources.add(source2);

        // Matches composite key but NOT where clause (region=EU)
        TestSource source3 = new TestSource();
        source3.setTargetId(1L);
        source3.setTargetCode("A");
        source3.setTargetVersion(300);
        source3.setTargetRegion("EU");
        sources.add(source3);

        // Setup data sources
        targetDataSource = new InMemoryDataSource<>("test-targets-where", TestTarget.class);
        targetDataSource.addItems(targets);
        
        sourceDataSource = new InMemoryDataSource<>("test-sources-where", TestSource.class);
        sourceDataSource.addItems(sources);

        factory.registerDataSource("test-targets-where", targetDataSource, Duration.ofMillis(500));
        factory.registerDataSource("test-sources-where", sourceDataSource, Duration.ofMillis(500));

        // When: Building store with composite key AND where clause
        InMemoryStoreBuilder<TestTarget> builder = factory.buildInMemoryStore(TestTargetSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestTarget.class);
        
        builder.target(TestTarget_.version)
                .from(TestSourceSpecificationService.INSTANCE, keys -> keys
                    .on(pk -> pk.field(TestTarget_.id), fk -> fk.field(TestSource_.targetId))
                    .on(pk -> pk.field(TestTarget_.code), fk -> fk.field(TestSource_.targetCode))
                )
                .where((nav, spec) -> spec.on(nav.field(TestSource_.targetRegion)).equalTo("US"))
                .sum(nav -> nav.field(TestSource_.targetVersion));
        
        InMemoryDataStore<TestTarget> store = builder.build();

        syncEngine = new DataSynchronizationEngine(factory);
        syncEngine.initialize();
        
        waitForSync();

        // Then: Verify only sources matching BOTH composite key AND where clause are aggregated
        List<TestTarget> results = store.findAll();
        assertThat(results).hasSize(1);

        TestTarget result = results.get(0);
        assertThat(result.getVersion())
                .as("Should only aggregate sources matching composite key AND where clause (100+200, not 300)")
                .isEqualTo(300);
    }

    /**
     * Test 15.5: Single-field mapping with new API
     * 
     * Requirements: 1.5
     * 
     * Create mapping with single on() call
     * Verify it works identically to composite with size=1
     */
    @Test
    @Order(5)
    void testSingleFieldMappingWithNewAPI() {
        // Given: Target and source entities
        List<TestTarget> targets = new ArrayList<>();
        TestTarget target1 = new TestTarget();
        target1.setId(1L);
        target1.setVersion(0);
        targets.add(target1);

        TestTarget target2 = new TestTarget();
        target2.setId(2L);
        target2.setVersion(0);
        targets.add(target2);

        List<TestSource> sources = new ArrayList<>();
        TestSource source1 = new TestSource();
        source1.setTargetId(1L);
        source1.setTargetVersion(100);
        sources.add(source1);

        TestSource source2 = new TestSource();
        source2.setTargetId(1L);
        source2.setTargetVersion(200);
        sources.add(source2);

        TestSource source3 = new TestSource();
        source3.setTargetId(2L);
        source3.setTargetVersion(300);
        sources.add(source3);

        // Setup data sources
        targetDataSource = new InMemoryDataSource<>("test-targets-single", TestTarget.class);
        targetDataSource.addItems(targets);
        
        sourceDataSource = new InMemoryDataSource<>("test-sources-single", TestSource.class);
        sourceDataSource.addItems(sources);

        factory.registerDataSource("test-targets-single", targetDataSource, Duration.ofMillis(500));
        factory.registerDataSource("test-sources-single", sourceDataSource, Duration.ofMillis(500));

        // When: Building store with single on() call (treated as composite with size=1)
        InMemoryStoreBuilder<TestTarget> builder = factory.buildInMemoryStore(TestTargetSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestTarget.class);
        
        builder.target(TestTarget_.version)
                .from(TestSourceSpecificationService.INSTANCE, keys -> keys
                    .on(pk -> pk.field(TestTarget_.id), fk -> fk.field(TestSource_.targetId))
                )
                .sum(nav -> nav.field(TestSource_.targetVersion));
        
        InMemoryDataStore<TestTarget> store = builder.build();

        syncEngine = new DataSynchronizationEngine(factory);
        syncEngine.initialize();
        
        waitForSync();

        // Then: Verify single-field mapping works correctly
        List<TestTarget> results = store.findAll();
        assertThat(results).hasSize(2);

        TestTarget result1 = results.stream()
                .filter(t -> t.getId().equals(1L))
                .findFirst()
                .orElseThrow();
        assertThat(result1.getVersion())
                .as("Single-field mapping should work like composite with size=1")
                .isEqualTo(300);

        TestTarget result2 = results.stream()
                .filter(t -> t.getId().equals(2L))
                .findFirst()
                .orElseThrow();
        assertThat(result2.getVersion())
                .as("Single-field mapping should work like composite with size=1")
                .isEqualTo(300);
    }

    /**
     * Test 15.6: Edge case - no matching records
     * 
     * Requirements: 3.5
     * 
     * Create scenario where no source records match composite key
     * Verify sum returns 0/null, count returns 0, value returns null
     */
    @Test
    @Order(6)
    void testEdgeCaseNoMatchingRecords() {
        // Given: Target entity with no matching sources
        List<TestTarget> targets = new ArrayList<>();
        TestTarget target1 = new TestTarget();
        target1.setId(1L);
        target1.setCode("A");
        target1.setVersion(0);
        targets.add(target1);

        // Given: Source entities that don't match
        List<TestSource> sources = new ArrayList<>();
        TestSource source1 = new TestSource();
        source1.setTargetId(2L);  // Different id
        source1.setTargetCode("A");
        source1.setTargetVersion(100);
        sources.add(source1);

        TestSource source2 = new TestSource();
        source2.setTargetId(1L);
        source2.setTargetCode("B");  // Different code
        source2.setTargetVersion(200);
        sources.add(source2);

        // Setup data sources
        targetDataSource = new InMemoryDataSource<>("test-targets-nomatch", TestTarget.class);
        targetDataSource.addItems(targets);
        
        sourceDataSource = new InMemoryDataSource<>("test-sources-nomatch", TestSource.class);
        sourceDataSource.addItems(sources);

        factory.registerDataSource("test-targets-nomatch", targetDataSource, Duration.ofMillis(500));
        factory.registerDataSource("test-sources-nomatch", sourceDataSource, Duration.ofMillis(500));

        // When: Building store with composite key mapping
        InMemoryStoreBuilder<TestTarget> builder = factory.buildInMemoryStore(TestTargetSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestTarget.class);
        
        builder.target(TestTarget_.version)
                .from(TestSourceSpecificationService.INSTANCE, keys -> keys
                    .on(pk -> pk.field(TestTarget_.id), fk -> fk.field(TestSource_.targetId))
                    .on(pk -> pk.field(TestTarget_.code), fk -> fk.field(TestSource_.targetCode))
                )
                .sum(nav -> nav.field(TestSource_.targetVersion));
        
        InMemoryDataStore<TestTarget> store = builder.build();

        syncEngine = new DataSynchronizationEngine(factory);
        syncEngine.initialize();
        
        waitForSync();

        // Then: Verify sum returns 0 when no matches
        List<TestTarget> results = store.findAll();
        assertThat(results).hasSize(1);

        TestTarget result = results.get(0);
        assertThat(result.getVersion())
                .as("Sum should return 0 when no sources match composite key")
                .isZero();
    }

    /**
     * Test 15.7: Edge case - no records match key and where clause
     * 
     * Requirements: 4.5
     * 
     * Create scenario where records match key but not where clause
     * Verify empty/null results
     */
    @Test
    @Order(7)
    void testEdgeCaseNoRecordsMatchKeyAndWhereClause() {
        // Given: Target entity
        List<TestTarget> targets = new ArrayList<>();
        TestTarget target1 = new TestTarget();
        target1.setId(1L);
        target1.setCode("A");
        target1.setVersion(0);
        targets.add(target1);

        // Given: Source entities that match composite key but NOT where clause
        List<TestSource> sources = new ArrayList<>();
        TestSource source1 = new TestSource();
        source1.setTargetId(1L);
        source1.setTargetCode("A");
        source1.setTargetVersion(100);
        source1.setTargetRegion("EU");  // Will not match where clause
        sources.add(source1);

        TestSource source2 = new TestSource();
        source2.setTargetId(1L);
        source2.setTargetCode("A");
        source2.setTargetVersion(200);
        source2.setTargetRegion("ASIA");  // Will not match where clause
        sources.add(source2);

        // Setup data sources
        targetDataSource = new InMemoryDataSource<>("test-targets-nowhere", TestTarget.class);
        targetDataSource.addItems(targets);
        
        sourceDataSource = new InMemoryDataSource<>("test-sources-nowhere", TestSource.class);
        sourceDataSource.addItems(sources);

        factory.registerDataSource("test-targets-nowhere", targetDataSource, Duration.ofMillis(500));
        factory.registerDataSource("test-sources-nowhere", sourceDataSource, Duration.ofMillis(500));

        // When: Building store with composite key AND where clause that filters out all matches
        InMemoryStoreBuilder<TestTarget> builder = factory.buildInMemoryStore(TestTargetSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestTarget.class);
        
        builder.target(TestTarget_.version)
                .from(TestSourceSpecificationService.INSTANCE, keys -> keys
                    .on(pk -> pk.field(TestTarget_.id), fk -> fk.field(TestSource_.targetId))
                    .on(pk -> pk.field(TestTarget_.code), fk -> fk.field(TestSource_.targetCode))
                )
                .where((nav, spec) -> spec.on(nav.field(TestSource_.targetRegion)).equalTo("US"))
                .sum(nav -> nav.field(TestSource_.targetVersion));
        
        InMemoryDataStore<TestTarget> store = builder.build();

        syncEngine = new DataSynchronizationEngine(factory);
        syncEngine.initialize();
        
        waitForSync();

        // Then: Verify sum returns 0 when sources match key but not where clause
        List<TestTarget> results = store.findAll();
        assertThat(results).hasSize(1);

        TestTarget result = results.get(0);
        assertThat(result.getVersion())
                .as("Sum should return 0 when sources match composite key but not where clause")
                .isZero();
    }
}
