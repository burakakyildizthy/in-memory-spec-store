package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Method;
import java.time.Instant;
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
 * Bug Condition Exploration Test — Cross-Datasource Mapping Streaming Pipeline Failure
 *
 * <p>Property 1: Fault Condition — Cross-datasource mapping'lerin streaming pipeline'da
 * çalışmaması. İki streaming datasource (birincil + ikincil) ile IncrementalSyncProcessor
 * kurulumu yapılıp, cross-datasource mapping'ler tanımlanıp, her iki datasource'a event
 * gönderildikten sonra root entity üzerindeki mapped alanlar kontrol edilir.</p>
 *
 * <p>Production lifecycle simulation: datasources start as INITIALIZING during initial
 * data load (fetchAll), Phase 2-4 are skipped. After data load completes, datasources
 * transition to READY. Cross-datasource mappings are never applied to the initial data
 * because Phase 2.5 was skipped during INITIALIZING — there is no catch-up mechanism.
 * Assertions check DependencyGraph entities directly since Phase 4 (store propagation)
 * is also skipped during INITIALIZING.</p>
 *
 * <p><b>BEKLENEN SONUÇ</b>: Bu testler fix'lenmemiş kodda BAŞARISIZ olmalıdır.
 * Başarısızlık, hatanın varlığını kanıtlar.</p>
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10</b></p>
 */
class CrossDatasourceMappingFaultConditionPropertyTest {

    private static final String CUSTOMER_DS = "customer-ds";
    private static final String ORDER_DS = "order-ds";
    private static final String ITEM_DS = "item-ds";
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
     * Register a streaming datasource mock in INITIALIZING state (simulates production
     * initializeStreamingInfrastructure — datasource registered before fetchAll).
     */
    @SuppressWarnings("unchecked")
    private <T extends Identifiable<?>> void registerInitializingStreamingDs(String dsName) {
        StreamingDataSource<T> ds = mock(StreamingDataSource.class);
        when(ds.getName()).thenReturn(dsName);
        when(ds.getState()).thenReturn(StreamingDataSourceState.INITIALIZING);
        factory.registerDataSource(dsName, ds);
        dsMocks.put(dsName, ds);
    }

    /**
     * Transition a datasource mock from INITIALIZING to READY (simulates production
     * post-fetchAll state transition).
     */
    private void transitionToReady(String dsName) {
        StreamingDataSource<?> ds = dsMocks.get(dsName);
        when(ds.getState()).thenReturn(StreamingDataSourceState.READY);
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


    // ==================== Test 1: ONE_TO_ONE Cross-Datasource Mapping ====================

    /**
     * ONE_TO_ONE: Customer.name should be populated from Order.status via cross-datasource FK mapping.
     *
     * <p>Bug condition: During INITIALIZING state, Phase 2-4 are skipped so cross-datasource
     * mappings are never applied to initial data. After transition to READY, there is no
     * catch-up mechanism. DependencyGraph entities lack mapped fields.</p>
     *
     * <p><b>Validates: Requirements 1.1, 1.2, 1.8</b></p>
     */
    @Example
    @Label("ONE_TO_ONE: Customer.name from Order.status — EXPECTED TO FAIL on unfixed code")
    void oneToOneMappingCrossDatasourceShouldPopulateField() throws Exception {
        registerInitializingStreamingDs(CUSTOMER_DS);
        registerInitializingStreamingDs(ORDER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        PropertyMapping<Customer, String> mapping = PropertyMapping.<Customer, String>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(ORDER_DS)
                .mappingType(MappingType.ONE_TO_ONE)
                .sourcePath(Collections.singletonList(Order_.status))
                .targetPath(Collections.singletonList(Customer_.name))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .sourceService(ORDER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        // === INITIALIZING phase: simulate fetchAll for both datasources ===
        // Phase 1 runs (data → DependencyGraph), Phase 2-4 SKIPPED
        Order order = new Order();
        order.setId(100L);
        order.setCustomerId(1L);
        order.setStatus("CONFIRMED");
        sendEvent(processor, ORDER_DS, List.of(order));

        Customer customer = new Customer();
        customer.setId(1L);
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        // === Transition to READY (post-fetchAll) ===
        transitionToReady(CUSTOMER_DS);
        transitionToReady(ORDER_DS);

        // === Post-initialization catch-up (simulates DataSynchronizationEngine behavior) ===
        processor.applyPostInitializationCatchUp();

        // Check DependencyGraph directly — Phase 2.5 was skipped during INITIALIZING,
        // so cross-datasource mapped fields should be missing
        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getName())
                .as("ONE_TO_ONE Bug: Customer.name should be 'CONFIRMED' from Order.status "
                        + "but Phase 2.5 was skipped during INITIALIZING and no catch-up runs. "
                        + "Counterexample: Customer(id=1, name=null)")
                .isEqualTo("CONFIRMED");
    }

    // ==================== Test 2: MANY_TO_ONE_AGGREGATION COUNT ====================

    /**
     * COUNT: Customer.totalOrders should be populated with COUNT of matching Orders.
     *
     * <p><b>Validates: Requirements 1.1, 1.10</b></p>
     */
    @Example
    @Label("COUNT: Customer.totalOrders should be 3 — EXPECTED TO FAIL on unfixed code")
    void countAggregationCrossDatasourceShouldPopulate() throws Exception {
        registerInitializingStreamingDs(CUSTOMER_DS);
        registerInitializingStreamingDs(ORDER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        PropertyMapping<Customer, Integer> mapping = PropertyMapping.<Customer, Integer>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(ORDER_DS)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .targetPath(Collections.singletonList(Customer_.totalOrders))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .sourceService(ORDER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        // === INITIALIZING: load 3 orders then customer ===
        for (long i = 100; i < 103; i++) {
            Order o = new Order();
            o.setId(i);
            o.setCustomerId(1L);
            o.setTotalAmount(50.0);
            sendEvent(processor, ORDER_DS, List.of(o));
        }

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setTotalOrders(0);
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        // === Transition to READY ===
        transitionToReady(CUSTOMER_DS);
        transitionToReady(ORDER_DS);
        processor.applyPostInitializationCatchUp();

        // Check DependencyGraph directly
        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getTotalOrders())
                .as("COUNT Bug: Customer.totalOrders should be 3 but Phase 2.5 was skipped "
                        + "during INITIALIZING. "
                        + "Counterexample: Customer(id=1, totalOrders=0)")
                .isEqualTo(3);
    }

    // ==================== Test 3: MANY_TO_ONE_AGGREGATION SUM ====================

    /**
     * SUM: Customer.totalSpent should be SUM of Order.totalAmount for matching orders.
     *
     * <p><b>Validates: Requirements 1.3, 1.10</b></p>
     */
    @Example
    @Label("SUM: Customer.totalSpent should be 150.0 — EXPECTED TO FAIL on unfixed code")
    void sumAggregationCrossDatasourceShouldPopulate() throws Exception {
        registerInitializingStreamingDs(CUSTOMER_DS);
        registerInitializingStreamingDs(ORDER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        PropertyMapping<Customer, Double> mapping = PropertyMapping.<Customer, Double>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(ORDER_DS)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .targetPath(Collections.singletonList(Customer_.totalSpent))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .sourceService(ORDER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        // === INITIALIZING ===
        for (long i = 100; i < 103; i++) {
            Order o = new Order();
            o.setId(i);
            o.setCustomerId(1L);
            o.setTotalAmount(50.0);
            sendEvent(processor, ORDER_DS, List.of(o));
        }

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setTotalSpent(0.0);
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        // === Transition to READY ===
        transitionToReady(CUSTOMER_DS);
        transitionToReady(ORDER_DS);
        processor.applyPostInitializationCatchUp();

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getTotalSpent())
                .as("SUM Bug: Customer.totalSpent should be 150.0 but Phase 2.5 was skipped "
                        + "during INITIALIZING. "
                        + "Counterexample: Customer(id=1, totalSpent=0.0)")
                .isEqualTo(150.0);
    }

    // ==================== Test 4: MANY_TO_ONE_AGGREGATION AVG ====================

    /**
     * AVG: Customer.averageOrderValue should be AVG of Order.totalAmount.
     *
     * <p><b>Validates: Requirements 1.4, 1.10</b></p>
     */
    @Example
    @Label("AVG: Customer.averageOrderValue should be 50.0 — EXPECTED TO FAIL on unfixed code")
    void avgAggregationCrossDatasourceShouldPopulate() throws Exception {
        registerInitializingStreamingDs(CUSTOMER_DS);
        registerInitializingStreamingDs(ORDER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        PropertyMapping<Customer, Double> mapping = PropertyMapping.<Customer, Double>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(ORDER_DS)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.AVG)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .targetPath(Collections.singletonList(Customer_.averageOrderValue))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .sourceService(ORDER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        // === INITIALIZING: 3 orders with avg = 50.0 ===
        double[] amounts = {30.0, 50.0, 70.0};
        for (int i = 0; i < amounts.length; i++) {
            Order o = new Order();
            o.setId((long) (100 + i));
            o.setCustomerId(1L);
            o.setTotalAmount(amounts[i]);
            sendEvent(processor, ORDER_DS, List.of(o));
        }

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setAverageOrderValue(0.0);
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        // === Transition to READY ===
        transitionToReady(CUSTOMER_DS);
        transitionToReady(ORDER_DS);
        processor.applyPostInitializationCatchUp();

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getAverageOrderValue())
                .as("AVG Bug: Customer.averageOrderValue should be 50.0 but Phase 2.5 was skipped "
                        + "during INITIALIZING. "
                        + "Counterexample: Customer(id=1, averageOrderValue=0.0)")
                .isEqualTo(50.0);
    }

    // ==================== Test 5: MANY_TO_ONE_AGGREGATION MIN ====================

    /**
     * MIN: Customer.totalSpent should be MIN of Order.totalAmount.
     *
     * <p><b>Validates: Requirements 1.5, 1.10</b></p>
     */
    @Example
    @Label("MIN: Customer.totalSpent should be 30.0 — EXPECTED TO FAIL on unfixed code")
    void minAggregationCrossDatasourceShouldPopulate() throws Exception {
        registerInitializingStreamingDs(CUSTOMER_DS);
        registerInitializingStreamingDs(ORDER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        PropertyMapping<Customer, Double> mapping = PropertyMapping.<Customer, Double>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(ORDER_DS)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.MIN)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .targetPath(Collections.singletonList(Customer_.totalSpent))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .sourceService(ORDER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        // === INITIALIZING ===
        double[] amounts = {30.0, 50.0, 70.0};
        for (int i = 0; i < amounts.length; i++) {
            Order o = new Order();
            o.setId((long) (100 + i));
            o.setCustomerId(1L);
            o.setTotalAmount(amounts[i]);
            sendEvent(processor, ORDER_DS, List.of(o));
        }

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setTotalSpent(0.0);
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        // === Transition to READY ===
        transitionToReady(CUSTOMER_DS);
        transitionToReady(ORDER_DS);
        processor.applyPostInitializationCatchUp();

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getTotalSpent())
                .as("MIN Bug: Customer.totalSpent should be 30.0 (min) but Phase 2.5 was skipped "
                        + "during INITIALIZING. "
                        + "Counterexample: Customer(id=1, totalSpent=0.0)")
                .isEqualTo(30.0);
    }

    // ==================== Test 6: MANY_TO_ONE_AGGREGATION MAX ====================

    /**
     * MAX: Customer.averageOrderValue should be MAX of Order.totalAmount.
     *
     * <p><b>Validates: Requirements 1.6, 1.10</b></p>
     */
    @Example
    @Label("MAX: Customer.averageOrderValue should be 70.0 — EXPECTED TO FAIL on unfixed code")
    void maxAggregationCrossDatasourceShouldPopulate() throws Exception {
        registerInitializingStreamingDs(CUSTOMER_DS);
        registerInitializingStreamingDs(ORDER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        PropertyMapping<Customer, Double> mapping = PropertyMapping.<Customer, Double>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(ORDER_DS)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.MAX)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .targetPath(Collections.singletonList(Customer_.averageOrderValue))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .sourceService(ORDER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        // === INITIALIZING ===
        double[] amounts = {30.0, 50.0, 70.0};
        for (int i = 0; i < amounts.length; i++) {
            Order o = new Order();
            o.setId((long) (100 + i));
            o.setCustomerId(1L);
            o.setTotalAmount(amounts[i]);
            sendEvent(processor, ORDER_DS, List.of(o));
        }

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setAverageOrderValue(0.0);
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        // === Transition to READY ===
        transitionToReady(CUSTOMER_DS);
        transitionToReady(ORDER_DS);
        processor.applyPostInitializationCatchUp();

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getAverageOrderValue())
                .as("MAX Bug: Customer.averageOrderValue should be 70.0 (max) but Phase 2.5 was skipped "
                        + "during INITIALIZING. "
                        + "Counterexample: Customer(id=1, averageOrderValue=0.0)")
                .isEqualTo(70.0);
    }


    // ==================== Test 7: MANY_TO_ONE_COLLECTION ====================

    /**
     * MANY_TO_ONE_COLLECTION: Customer.orders should be populated with matching Order entities.
     *
     * <p><b>Validates: Requirements 1.7, 1.10</b></p>
     */
    @Example
    @Label("COLLECTION: Customer.orders should have 2 orders — EXPECTED TO FAIL on unfixed code")
    void collectionMappingCrossDatasourceShouldPopulate() throws Exception {
        registerInitializingStreamingDs(CUSTOMER_DS);
        registerInitializingStreamingDs(ORDER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        PropertyMapping<Customer, Order> mapping = PropertyMapping.<Customer, Order>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(ORDER_DS)
                .mappingType(MappingType.MANY_TO_ONE_COLLECTION)
                .targetPath(Collections.singletonList(Customer_.orders))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .sourceService(ORDER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        // === INITIALIZING ===
        Order o1 = new Order();
        o1.setId(100L);
        o1.setCustomerId(1L);
        o1.setStatus("CONFIRMED");
        sendEvent(processor, ORDER_DS, List.of(o1));

        Order o2 = new Order();
        o2.setId(101L);
        o2.setCustomerId(1L);
        o2.setStatus("SHIPPED");
        sendEvent(processor, ORDER_DS, List.of(o2));

        Customer customer = new Customer();
        customer.setId(1L);
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        // === Transition to READY ===
        transitionToReady(CUSTOMER_DS);
        transitionToReady(ORDER_DS);

        processor.applyPostInitializationCatchUp();

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getOrders())
                .as("COLLECTION Bug: Customer.orders should have 2 orders but Phase 2.5 was skipped "
                        + "during INITIALIZING. "
                        + "Counterexample: Customer(id=1, orders=null or empty)")
                .isNotNull()
                .hasSize(2);
    }

    // ==================== Test 8: ONE_TO_ONE value() mapping ====================

    /**
     * value(): Order.totalAmount should be populated from OrderItem.unitPrice via ONE_TO_ONE.
     * Tests cross-datasource value extraction (not aggregation).
     *
     * <p><b>Validates: Requirements 1.2, 1.8, 1.10</b></p>
     */
    @Example
    @Label("VALUE: Order.totalAmount from OrderItem.unitPrice — EXPECTED TO FAIL on unfixed code")
    void valueMappingCrossDatasourceShouldPopulate() throws Exception {
        registerInitializingStreamingDs(ORDER_DS);
        registerInitializingStreamingDs(ITEM_DS);

        InMemoryDataStore<Order> store = new InMemoryDataStore<>(
                Order.class, ORDER_STORE, ORDER_DS, null, Collections.emptyList());
        registerStore(store);

        PropertyMapping<Order, Double> mapping = PropertyMapping.<Order, Double>builder()
                .consumerId(ORDER_STORE)
                .isForDashboard(false)
                .datasourceName(ITEM_DS)
                .mappingType(MappingType.ONE_TO_ONE)
                .sourcePath(Collections.singletonList(OrderItem_.unitPrice))
                .targetPath(Collections.singletonList(Order_.totalAmount))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Order_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(OrderItem_.orderId)))
                .sourceService(ITEM_SVC)
                .targetService(ORDER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        // === INITIALIZING ===
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrderId(10L);
        item.setUnitPrice(99.99);
        sendEvent(processor, ITEM_DS, List.of(item));

        Order order = new Order();
        order.setId(10L);
        sendEvent(processor, ORDER_DS, List.of(order));

        // === Transition to READY ===
        transitionToReady(ORDER_DS);
        transitionToReady(ITEM_DS);
        processor.applyPostInitializationCatchUp();

        List<Order> graphData = graph.findAll(ORDER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getTotalAmount())
                .as("VALUE Bug: Order.totalAmount should be 99.99 from OrderItem.unitPrice "
                        + "but Phase 2.5 was skipped during INITIALIZING. "
                        + "Counterexample: Order(id=10, totalAmount=null)")
                .isEqualTo(99.99);
    }

    // ==================== Test 9: Collection Selector FIRST ====================

    /**
     * FIRST collection selector: Customer.totalSpent from Order.items collection using FIRST selector.
     * Tests that collection selector operations work with cross-datasource mappings.
     *
     * <p><b>Validates: Requirements 1.9, 1.10</b></p>
     */
    @Example
    @Label("FIRST selector: Order.totalAmount from first OrderItem — EXPECTED TO FAIL on unfixed code")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void firstCollectionSelectorCrossDatasourceShouldWork() throws Exception {
        registerInitializingStreamingDs(CUSTOMER_DS);
        registerInitializingStreamingDs(ORDER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        List sourceCollOps = new ArrayList();
        sourceCollOps.add(new CollectionOperationMetadata<>(
                0, // pathIndex for Order_.items
                Order_.items,
                CollectionSelector.FIRST,
                null // no specification filter
        ));

        PropertyMapping<Customer, Double> mapping = PropertyMapping.<Customer, Double>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(ORDER_DS)
                .mappingType(MappingType.ONE_TO_ONE)
                .sourcePath(List.of(Order_.items, OrderItem_.unitPrice))
                .targetPath(Collections.singletonList(Customer_.totalSpent))
                .sourceCollectionOperations(sourceCollOps)
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .sourceService(ORDER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        // === INITIALIZING ===
        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrderId(100L);
        item1.setUnitPrice(25.0);

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrderId(100L);
        item2.setUnitPrice(75.0);

        Order order = new Order();
        order.setId(100L);
        order.setCustomerId(1L);
        order.setItems(List.of(item1, item2));
        sendEvent(processor, ORDER_DS, List.of(order));

        Customer customer = new Customer();
        customer.setId(1L);
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        // === Transition to READY ===
        transitionToReady(CUSTOMER_DS);
        transitionToReady(ORDER_DS);
        processor.applyPostInitializationCatchUp();

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getTotalSpent())
                .as("FIRST Selector Bug: Customer.totalSpent should be 25.0 (first item's unitPrice) "
                        + "but Phase 2.5 was skipped during INITIALIZING. "
                        + "Counterexample: Customer(id=1, totalSpent=null)")
                .isNotNull()
                .isEqualTo(25.0);
    }

    // ==================== Test 10: Property-Based — Random Cross-Datasource COUNT ====================

    /**
     * Property-based test: For any number of orders (1-10) associated with a customer,
     * the cross-datasource COUNT aggregation should return the correct count after
     * INITIALIZING → data load → READY lifecycle.
     *
     * <p><b>Validates: Requirements 1.1, 1.10</b></p>
     */
    @Property(tries = 20)
    @Label("Property: COUNT(orders) = actual order count — EXPECTED TO FAIL on unfixed code")
    void propertyCountAggregationMatchesActualCount(
            @ForAll("orderCounts") int orderCount) throws Exception {

        factory.clearAll();
        dsMocks.clear();

        registerInitializingStreamingDs(CUSTOMER_DS);
        registerInitializingStreamingDs(ORDER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        PropertyMapping<Customer, Integer> mapping = PropertyMapping.<Customer, Integer>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(ORDER_DS)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .targetPath(Collections.singletonList(Customer_.totalOrders))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .sourceService(ORDER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        // === INITIALIZING ===
        for (int i = 0; i < orderCount; i++) {
            Order o = new Order();
            o.setId((long) (100 + i));
            o.setCustomerId(1L);
            o.setTotalAmount(10.0 * (i + 1));
            sendEvent(processor, ORDER_DS, List.of(o));
        }

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setTotalOrders(0);
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        // === Transition to READY ===
        transitionToReady(CUSTOMER_DS);
        transitionToReady(ORDER_DS);
        processor.applyPostInitializationCatchUp();

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getTotalOrders())
                .as("Property COUNT Bug: Customer.totalOrders should be %d but Phase 2.5 was skipped "
                        + "during INITIALIZING. "
                        + "Counterexample: orderCount=%d, Customer.totalOrders=%s",
                        orderCount, orderCount, graphData.get(0).getTotalOrders())
                .isEqualTo(orderCount);
    }

    @Provide
    Arbitrary<Integer> orderCounts() {
        return Arbitraries.integers().between(1, 10);
    }


    // ==================== Test 11: Foreign Event Arrives — Reverse Order ====================

    /**
     * Reverse event order with INITIALIZING lifecycle: Primary entity arrives first during
     * INITIALIZING, then foreign entity arrives during INITIALIZING. After transition to READY,
     * the DependencyGraph entity should have mapped values but doesn't due to skipped Phase 2.5.
     *
     * <p><b>Validates: Requirements 1.1, 1.2, 1.8, 1.10</b></p>
     */
    @Example
    @Label("REVERSE ORDER: Foreign event after primary — ONE_TO_ONE should update — EXPECTED TO FAIL")
    void reverseOrderForeignEventAfterPrimaryShouldUpdateMapping() throws Exception {
        registerInitializingStreamingDs(CUSTOMER_DS);
        registerInitializingStreamingDs(ORDER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        PropertyMapping<Customer, String> mapping = PropertyMapping.<Customer, String>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(ORDER_DS)
                .mappingType(MappingType.ONE_TO_ONE)
                .sourcePath(Collections.singletonList(Order_.status))
                .targetPath(Collections.singletonList(Customer_.name))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .sourceService(ORDER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        // === INITIALIZING: primary first, then foreign ===
        Customer customer = new Customer();
        customer.setId(1L);
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        Order order = new Order();
        order.setId(100L);
        order.setCustomerId(1L);
        order.setStatus("CONFIRMED");
        sendEvent(processor, ORDER_DS, List.of(order));

        // === Transition to READY ===
        transitionToReady(CUSTOMER_DS);
        transitionToReady(ORDER_DS);
        processor.applyPostInitializationCatchUp();

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getName())
                .as("REVERSE ORDER Bug: After INITIALIZING→READY transition, Customer.name should be "
                        + "'CONFIRMED' but Phase 2.5 was skipped during INITIALIZING. "
                        + "Counterexample: Customer(id=1, name=null)")
                .isEqualTo("CONFIRMED");
    }

    // ==================== Test 12: Foreign Event — COUNT should update ====================

    /**
     * Foreign events during INITIALIZING: Orders arrive after Customer during INITIALIZING.
     * After READY transition, DependencyGraph Customer.totalOrders should reflect the count.
     *
     * <p><b>Validates: Requirements 1.1, 1.10</b></p>
     */
    @Example
    @Label("REVERSE ORDER: Foreign events trigger COUNT update — EXPECTED TO FAIL")
    void reverseOrderForeignEventsShouldUpdateCount() throws Exception {
        registerInitializingStreamingDs(CUSTOMER_DS);
        registerInitializingStreamingDs(ORDER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        PropertyMapping<Customer, Integer> mapping = PropertyMapping.<Customer, Integer>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(ORDER_DS)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.COUNT)
                .targetPath(Collections.singletonList(Customer_.totalOrders))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .sourceService(ORDER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        // === INITIALIZING: primary first, then 3 foreign entities ===
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setTotalOrders(0);
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        for (long i = 100; i < 103; i++) {
            Order o = new Order();
            o.setId(i);
            o.setCustomerId(1L);
            o.setTotalAmount(50.0);
            sendEvent(processor, ORDER_DS, List.of(o));
        }

        // === Transition to READY ===
        transitionToReady(CUSTOMER_DS);
        transitionToReady(ORDER_DS);
        processor.applyPostInitializationCatchUp();

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getTotalOrders())
                .as("REVERSE ORDER COUNT Bug: Customer.totalOrders should be 3 after INITIALIZING→READY "
                        + "but Phase 2.5 was skipped during INITIALIZING. "
                        + "Counterexample: Customer(id=1, totalOrders=%s)", graphData.get(0).getTotalOrders())
                .isEqualTo(3);
    }

    // ==================== Test 13: Foreign Event — SUM should update ====================

    /**
     * Foreign events during INITIALIZING trigger SUM: Orders arrive after Customer during INITIALIZING.
     * After READY transition, DependencyGraph Customer.totalSpent should reflect the sum.
     *
     * <p><b>Validates: Requirements 1.3, 1.10</b></p>
     */
    @Example
    @Label("REVERSE ORDER: Foreign events trigger SUM update — EXPECTED TO FAIL")
    void reverseOrderForeignEventsShouldUpdateSum() throws Exception {
        registerInitializingStreamingDs(CUSTOMER_DS);
        registerInitializingStreamingDs(ORDER_DS);

        InMemoryDataStore<Customer> store = new InMemoryDataStore<>(
                Customer.class, CUSTOMER_STORE, CUSTOMER_DS, null, Collections.emptyList());
        registerStore(store);

        PropertyMapping<Customer, Double> mapping = PropertyMapping.<Customer, Double>builder()
                .consumerId(CUSTOMER_STORE)
                .isForDashboard(false)
                .datasourceName(ORDER_DS)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(AggregationType.SUM)
                .sourcePath(Collections.singletonList(Order_.totalAmount))
                .targetPath(Collections.singletonList(Customer_.totalSpent))
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(Customer_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.customerId)))
                .sourceService(ORDER_SVC)
                .targetService(CUSTOMER_SVC)
                .build();

        DependencyGraph graph = new DependencyGraph();
        graph.addMapping(mapping);

        IncrementalSyncProcessor processor = createProcessor(graph);

        // === INITIALIZING: primary first, then foreign entities ===
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setTotalSpent(0.0);
        sendEvent(processor, CUSTOMER_DS, List.of(customer));

        double[] amounts = {30.0, 50.0, 70.0};
        for (int i = 0; i < amounts.length; i++) {
            Order o = new Order();
            o.setId((long) (100 + i));
            o.setCustomerId(1L);
            o.setTotalAmount(amounts[i]);
            sendEvent(processor, ORDER_DS, List.of(o));
        }

        // === Transition to READY ===
        transitionToReady(CUSTOMER_DS);
        transitionToReady(ORDER_DS);
        processor.applyPostInitializationCatchUp();

        List<Customer> graphData = graph.findAll(CUSTOMER_DS);
        assertThat(graphData).hasSize(1);
        assertThat(graphData.get(0).getTotalSpent())
                .as("REVERSE ORDER SUM Bug: Customer.totalSpent should be 150.0 after INITIALIZING→READY "
                        + "but Phase 2.5 was skipped during INITIALIZING. "
                        + "Counterexample: Customer(id=1, totalSpent=%s)",
                        graphData.get(0).getTotalSpent())
                .isEqualTo(150.0);
    }
}
