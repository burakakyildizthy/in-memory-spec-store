package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.CollectionSelector;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.Customer;
import com.thy.fss.common.inmemory.testmodel.Customer_;
import com.thy.fss.common.inmemory.testmodel.CustomerSpecificationService;
import com.thy.fss.common.inmemory.testmodel.Order;
import com.thy.fss.common.inmemory.testmodel.Order_;
import com.thy.fss.common.inmemory.testmodel.OrderSpecificationService;
import com.thy.fss.common.inmemory.testmodel.OrderItem;
import com.thy.fss.common.inmemory.testmodel.OrderItem_;
import com.thy.fss.common.inmemory.testmodel.OrderItemSpecificationService;

/**
 * Preservation Property Tests — Mevcut Davranışların Korunması
 *
 * <p>Property 2: Preservation — Non-buggy scenarios that MUST continue working after the fix.
 * These tests observe and lock down behavior on UNFIXED code for cases where
 * {@code isBugCondition} returns false:</p>
 * <ul>
 *   <li>Same-datasource mappings in READY state (Phase 2-4 run normally)</li>
 *   <li>Primary datasource fields preserved through processing</li>
 *   <li>INITIALIZING state correctly skips Phase 2-4</li>
 * </ul>
 *
 * <p><b>ALL tests MUST PASS on unfixed code.</b></p>
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10, 3.11, 3.12, 3.13</b></p>
 */
class CrossDatasourceMappingPreservationPropertyTest {

    private static final String CUSTOMER_DS = "customer-ds";
    private static final String ORDER_DS = "order-ds";
    private static final String CUSTOMER_STORE = "customer-store";
    private static final String ORDER_STORE = "order-store";

    private static final SpecificationService<Customer> CUSTOMER_SVC = CustomerSpecificationService.INSTANCE;
    private static final SpecificationService<Order> ORDER_SVC = OrderSpecificationService.INSTANCE;
    private static final SpecificationService<OrderItem> ITEM_SVC = OrderItemSpecificationService.INSTANCE;

    private InMemorySpecStoreFactory factory;
    private final Map<String, StreamingDataSource<?>> dsMocks = new HashMap<>();

    @BeforeProperty
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();
        dsMocks.clear();
    }

    @AfterProperty
    void tearDown() {
        factory.clearAll();
        dsMocks.clear();
    }

    // ==================== Helpers ====================

    /**
     * Register a streaming datasource mock directly in READY state.
     * For preservation tests, same-datasource mappings work when datasource is READY.
     */
    @SuppressWarnings("unchecked")
    private <T extends Identifiable<?>> void registerReadyStreamingDs(String dsName) {
        StreamingDataSource<T> ds = mock(StreamingDataSource.class);
        when(ds.getName()).thenReturn(dsName);
        when(ds.getState()).thenReturn(StreamingDataSourceState.READY);
        factory.registerDataSource(dsName, ds);
        dsMocks.put(dsName, ds);
    }

    /**
     * Register a streaming datasource mock in INITIALIZING state.
     */
    @SuppressWarnings("unchecked")
    private <T extends Identifiable<?>> void registerInitializingStreamingDs(String dsName) {
        StreamingDataSource<T> ds = mock(StreamingDataSource.class);
        when(ds.getName()).thenReturn(dsName);
        when(ds.getState()).thenReturn(StreamingDataSourceState.INITIALIZING);
        factory.registerDataSource(dsName, ds);
        dsMocks.put(dsName, ds);
    }

    private void registerStore(InMemoryDataStore<?> store) throws Exception {
        Method m = InMemorySpecStoreFactory.class.getDeclaredMethod("registerStore", InMemoryDataStore.class);
        m.setAccessible(true);
        m.invoke(factory, store);
    }

    private IncrementalSyncProcessor createProcessor(DependencyGraph graph) {
        AnalysisResult analysisResult = new AnalysisResult(null, null, null);
        AtomicLong streamingVersion = new AtomicLong(0);
        return new IncrementalSyncProcessor(factory, graph, analysisResult, streamingVersion);
    }

    private <T extends Identifiable<?>> void sendEvent(
            IncrementalSyncProcessor processor, String dsName, List<T> entities) {
        BatchSnapshotEvent<T> event = new BatchSnapshotEvent<>(entities, Instant.now());
        processor.processBatchSnapshot(dsName, event);
    }


    // ==================== Test 1: Same-datasource ONE_TO_ONE mapping in READY state ====================

    /**
     * Preservation: Same-datasource ONE_TO_ONE mapping works correctly in READY state.
     * Source and target are in the SAME datasource — Phase 2.5 runs and applies the mapping.
     *
     * <p><b>Validates: Requirements 3.2, 3.10</b></p>
     */
    @Example
    @Label("PRESERVATION: Same-datasource ONE_TO_ONE mapping works in READY state")
    void sameDatasourceOneToOneWorksInReadyState() throws Exception {
        // Single datasource registered as READY
        registerReadyStreamingDs(CUSTOMER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        // Same-datasource mapping: Customer.name from Customer.email (same datasource)
        PropertyMapping<Customer, String> mapping = PropertyMapping.<Customer, String>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(CUSTOMER_DS)  // SAME datasource
                .mappingType(MappingType.ONE_TO_ONE)
                .sourcePath(Collections.singletonList(Customer_.email))
                .targetPath(Collections.singletonList(Customer_.name))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .sourceService(CUSTOMER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setEmail("test@example.com");
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getName())
                .as("Same-datasource ONE_TO_ONE: Customer.name should be populated from Customer.email")
                .isEqualTo("test@example.com");
    }

    // ==================== Test 2: Same-datasource COUNT aggregation in READY state ====================

    /**
     * Preservation: Same-datasource COUNT aggregation works correctly in READY state.
     * Both source and target entities are in the same datasource.
     *
     * <p><b>Validates: Requirements 3.2</b></p>
     */
    @Example
    @Label("PRESERVATION: Same-datasource COUNT aggregation works in READY state")
    void sameDatasourceCountAggregationWorksInReadyState() throws Exception {
        registerReadyStreamingDs(ORDER_DS);

        InMemoryDataStore<Order> store = new InMemoryDataStore<>(
                Order.class, ORDER_STORE, ORDER_DS, null, Collections.emptyList());
        registerStore(store);

        // Same-datasource COUNT: Order.totalAmount = COUNT of Orders matching by customerId
        // This is a self-referencing aggregation within the same datasource
        PropertyMapping<Order, Integer> mapping = PropertyMapping.<Order, Integer>builder()
                .consumerId(ORDER_STORE)
                .isForDashboard(false)
                .datasourceName(ORDER_DS)  // SAME datasource
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .targetPath(Collections.singletonList(Order_.totalAmount))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .sourceService(ORDER_SVC)
                .targetService(ORDER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        // Send 3 orders with same customerId
        for (long i = 1; i <= 3; i++) {
            Order o = new Order();
            o.setId(i);
            o.setCustomerId(1L);
            sendEvent(processor, ORDER_DS, List.of(o));
        }

        List<Order> graphData = graph.findAll(ORDER_DS);
        assertThat(graphData).hasSize(3);
        // Each order should have count = 3 (all 3 orders match customerId=1)
        for (Order o : graphData) {
            assertThat(o.getTotalAmount())
                    .as("Same-datasource COUNT: Order.totalAmount should be 3.0 (count of orders with same customerId)")
                    .isEqualTo(3.0);
        }
    }

    // ==================== Test 3: Same-datasource SUM aggregation in READY state ====================

    /**
     * Preservation: Same-datasource SUM aggregation works correctly in READY state.
     *
     * <p><b>Validates: Requirements 3.2</b></p>
     */
    @Example
    @Label("PRESERVATION: Same-datasource SUM aggregation works in READY state")
    void sameDatasourceSumAggregationWorksInReadyState() throws Exception {
        registerReadyStreamingDs(CUSTOMER_DS);
        registerReadyStreamingDs(ORDER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        // Same-datasource SUM: Customer.totalSpent = SUM of Customer.averageOrderValue
        // Self-referencing within same datasource
        PropertyMapping<Customer, Double> mapping = PropertyMapping.<Customer, Double>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(CUSTOMER_DS)  // SAME datasource
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .sourcePath(Collections.singletonList(Customer_.averageOrderValue))
                .targetPath(Collections.singletonList(Customer_.totalSpent))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.active)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.active)))
                .sourceService(CUSTOMER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        Customer c1 = new Customer();
        c1.setId(1L);
        c1.setActive(true);
        c1.setAverageOrderValue(30.0);
        sendEvent(processor, CUSTOMER_DS, List.of(c1));

        Customer c2 = new Customer();
        c2.setId(2L);
        c2.setActive(true);
        c2.setAverageOrderValue(70.0);
        sendEvent(processor, CUSTOMER_DS, List.of(c2));

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(2);
        // Each customer with active=true should have totalSpent = 30.0 + 70.0 = 100.0
        for (Customer c : graphData) {
            assertThat(c.getTotalSpent())
                    .as("Same-datasource SUM: Customer.totalSpent should be 100.0 (sum of averageOrderValue for active=true)")
                    .isEqualTo(100.0);
        }
    }

    // ==================== Test 4: Same-datasource MANY_TO_ONE_COLLECTION in READY state ====================

    /**
     * Preservation: Same-datasource MANY_TO_ONE_COLLECTION mapping works correctly in READY state.
     *
     * <p><b>Validates: Requirements 3.9</b></p>
     */
    @Example
    @Label("PRESERVATION: Same-datasource MANY_TO_ONE_COLLECTION works in READY state")
    void sameDatasourceCollectionWorksInReadyState() throws Exception {
        registerReadyStreamingDs(CUSTOMER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        // Same-datasource collection: Customer.orders populated from other Customers
        // matching by active flag (self-referencing collection within same datasource)
        PropertyMapping<Customer, Customer> mapping = PropertyMapping.<Customer, Customer>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(CUSTOMER_DS)  // SAME datasource
                .mappingType(MappingType.MANY_TO_ONE_COLLECTION)
                .targetPath(Collections.singletonList(Customer_.orders))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.active)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.active)))
                .sourceService(CUSTOMER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        Customer c1 = new Customer();
        c1.setId(1L);
        c1.setActive(true);

        Customer c2 = new Customer();
        c2.setId(2L);
        c2.setActive(true);

        // Send both in a single batch so Phase 2.5 sees all entities
        sendEvent(processor, CUSTOMER_DS, List.of(c1, c2));

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(2);
        // Each customer should have orders collection populated (both match active=true)
        for (Customer c : graphData) {
            assertThat(c.getOrders())
                    .as("Same-datasource COLLECTION: Customer.orders should be populated with matching entities")
                    .isNotNull()
                    .hasSizeGreaterThanOrEqualTo(1);
        }
    }


    // ==================== Test 5: Primary datasource fields preserved ====================

    /**
     * Preservation: Primary datasource fields (id, name, email, registrationDate, active)
     * are preserved through processing. Fields set on the entity before sending should
     * remain unchanged in DependencyGraph after Phase 1.
     *
     * <p><b>Validates: Requirements 3.1</b></p>
     */
    @Example
    @Label("PRESERVATION: Primary datasource fields preserved through processing")
    void primaryDatasourceFieldsPreservedAfterProcessing() throws Exception {
        registerReadyStreamingDs(CUSTOMER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        DependencyGraph graph = new DependencyGraph();
        IncrementalSyncProcessor processor = createProcessor(graph);

        LocalDateTime regDate = LocalDateTime.of(2024, 1, 15, 10, 30);
        Customer customer = new Customer();
        customer.setId(42L);
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setRegistrationDate(regDate);
        customer.setActive(true);
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);

        Customer stored = graphData.get(0);
        assertThat(stored.getIdentity()).as("id preserved").isEqualTo(42L);
        assertThat(stored.getName()).as("name preserved").isEqualTo("John Doe");
        assertThat(stored.getEmail()).as("email preserved").isEqualTo("john@example.com");
        assertThat(stored.getRegistrationDate()).as("registrationDate preserved").isEqualTo(regDate);
        assertThat(stored.getActive()).as("active preserved").isTrue();
    }

    // ==================== Test 6: INITIALIZING state skips Phase 2-4 ====================

    /**
     * Preservation: When datasource is INITIALIZING, Phase 1 runs (entities stored in
     * DependencyGraph) but Phase 2-4 are skipped. This means same-datasource mappings
     * are NOT applied during INITIALIZING — this is CORRECT behavior to preserve.
     *
     * <p><b>Validates: Requirements 3.5</b></p>
     */
    @Example
    @Label("PRESERVATION: INITIALIZING state skips Phase 2-4 — entities stored but mappings not applied")
    void initializingStateSkipsPhase2to4() throws Exception {
        registerInitializingStreamingDs(CUSTOMER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        // Same-datasource mapping that would normally run in READY state
        PropertyMapping<Customer, String> mapping = PropertyMapping.<Customer, String>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(CUSTOMER_DS)
                .mappingType(MappingType.ONE_TO_ONE)
                .sourcePath(Collections.singletonList(Customer_.email))
                .targetPath(Collections.singletonList(Customer_.name))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .sourceService(CUSTOMER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setEmail("test@example.com");
        // name is NOT set — if Phase 2.5 ran, it would be set to email value
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        // Phase 1 ran: entity is in DependencyGraph
        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);

        // Phase 2.5 was SKIPPED: name should NOT be populated from email
        assertThat(graphData.get(0).getName())
                .as("INITIALIZING: Phase 2.5 skipped — Customer.name should NOT be mapped from email")
                .isNull();

        // But the entity's own fields are preserved (Phase 1 ran)
        assertThat(graphData.get(0).getEmail())
                .as("INITIALIZING: Phase 1 ran — Customer.email should be preserved")
                .isEqualTo("test@example.com");
    }

    // ==================== Test 7: Same-datasource FIRST collection selector in READY state ====================

    /**
     * Preservation: Same-datasource mapping with FIRST collection selector works in READY state.
     * Tests that collection selector operations work for same-datasource mappings.
     *
     * <p><b>Validates: Requirements 3.11, 3.13</b></p>
     */
    @Example
    @Label("PRESERVATION: Same-datasource FIRST collection selector works in READY state")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void sameDatasourceFirstSelectorWorksInReadyState() throws Exception {
        registerReadyStreamingDs(ORDER_DS);

        InMemoryDataStore<Order> store = new InMemoryDataStore<>(
                Order.class, ORDER_STORE, ORDER_DS, null, Collections.emptyList());
        registerStore(store);

        // Same-datasource mapping with FIRST selector: Order.status from Order.items.first().unitPrice
        // Source path navigates through items collection with FIRST selector
        List sourceCollOps = new ArrayList();
        sourceCollOps.add(new CollectionOperationMetadata<>(
                0, // pathIndex for Order_.items
                Order_.items,
                CollectionSelector.FIRST,
                null // no specification filter
        ));

        PropertyMapping<Order, Double> mapping = PropertyMapping.<Order, Double>builder()
                .consumerId(ORDER_STORE)
                .isForDashboard(false)
                .datasourceName(ORDER_DS)  // SAME datasource
                .mappingType(MappingType.ONE_TO_ONE)
                .sourcePath(List.of(Order_.items, OrderItem_.unitPrice))
                .targetPath(Collections.singletonList(Order_.totalAmount))
                .sourceCollectionOperations(sourceCollOps)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.id)))
                .sourceService(ORDER_SVC)
                .targetService(ORDER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrderId(10L);
        item1.setUnitPrice(25.0);

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrderId(10L);
        item2.setUnitPrice(75.0);

        Order order = new Order();
        order.setId(10L);
        order.setCustomerId(1L);
        order.setItems(List.of(item1, item2));
        sendEvent(processor, ORDER_DS, List.of(order));

        List<Order> graphData = graph.findAll(ORDER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getTotalAmount())
                .as("Same-datasource FIRST selector: Order.totalAmount should be 25.0 (first item's unitPrice)")
                .isNotNull()
                .isEqualTo(25.0);
    }

    // ==================== Property Test: Primary fields preserved for random inputs ====================

    /**
     * Property-based test: For any customer with random field values, primary datasource
     * fields are preserved through processing in READY state.
     *
     * <p><b>Validates: Requirements 3.1</b></p>
     */
    @Property(tries = 20)
    @Label("Property: Primary datasource fields preserved for random inputs")
    void propertyPrimaryFieldsPreserved(
            @ForAll("customerIds") long customerId,
            @ForAll("customerNames") String customerName) throws Exception {

        factory.clearAll();
        dsMocks.clear();

        registerReadyStreamingDs(CUSTOMER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        DependencyGraph graph = new DependencyGraph();
        IncrementalSyncProcessor processor = createProcessor(graph);

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName(customerName);
        customer.setActive(true);
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getIdentity())
                .as("Property: id preserved for customerId=%d", customerId)
                .isEqualTo(customerId);
        assertThat(graphData.get(0).getName())
                .as("Property: name preserved for customerName=%s", customerName)
                .isEqualTo(customerName);
        assertThat(graphData.get(0).getActive())
                .as("Property: active preserved")
                .isTrue();
    }

    @Provide
    Arbitrary<Long> customerIds() {
        return Arbitraries.longs().between(1, 10000);
    }

    @Provide
    Arbitrary<String> customerNames() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
    }
}
