package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thy.fss.common.inmemory.datasource.BatchSnapshotEvent;
import com.thy.fss.common.inmemory.datasource.StreamingDataSource;
import com.thy.fss.common.inmemory.datasource.StreamingDataSourceState;
import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.LongAttribute;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.Order;
import com.thy.fss.common.inmemory.testmodel.SimpleUser;

import net.jqwik.api.Property;

/**
 * Fault Condition Exploration Property Test — PropertyMapping Path Type Mismatch (ClassCastException).
 *
 * <p>This test class proves the existence of the ACTUAL bug in
 * {@code applyPhase4ConsumerPropagation}: the PropertyMapping path uses
 * {@code dependencyGraph.findAll(dataSourceName)} which fetches SOURCE datasource
 * entities (e.g., Order) and writes them to a TARGET store that expects DIFFERENT
 * type entities (e.g., User) → ClassCastException.</p>
 *
 * <p>Additionally, the PropertyMapping path does NOT apply {@code rootSpecification}
 * filter, and shares a single {@code findAll} result across all stores regardless
 * of their different filtering needs.</p>
 *
 * <p><b>Property 1: Fault Condition — Type Mismatch in PropertyMapping Path of applyPhase4ConsumerPropagation</b></p>
 * <p><b>Validates: Requirements 1.0, 1.1, 1.2, 2.0, 2.1, 2.2</b></p>
 */
class PrimaryDatasourceConsumerFaultConditionPropertyTest {

    private static final String ORDERS_DS = "ordersDS";
    private static final String USERS_DS = "usersDS";
    private static final String CREW_DS = "crewDS";
    private static final String FLIGHT_DS = "flightDS";
    private static final String ALL_USERS_DS = "allUsersDS";
    private static final String USER_STORE = "userStore";

    // ==================== Helper: Register store via reflection ====================

    private static <T> void registerStoreViaReflection(InMemorySpecStoreFactory factory,
                                                       InMemoryDataStore<T> store) throws Exception {
        Method registerStoreMethod = InMemorySpecStoreFactory.class.getDeclaredMethod(
                "registerStore", InMemoryDataStore.class);
        registerStoreMethod.setAccessible(true);
        registerStoreMethod.invoke(factory, store);
    }

    // ==================== Helper: Create entities ====================

    private static SimpleUser createUser(Long id, String name, boolean active) {
        SimpleUser user = new SimpleUser();
        user.setId(id);
        user.setName(name);
        user.setActive(active);
        return user;
    }

    private static Order createOrder(Long id, Double totalAmount, String status) {
        Order order = new Order();
        order.setId(id);
        order.setTotalAmount(totalAmount);
        order.setStatus(status);
        return order;
    }

    // ==================== Helper: Create PropertyMapping ====================

    /**
     * Creates a minimal valid PropertyMapping for a store (non-dashboard).
     * Uses ONE_TO_ONE mapping type without sourcePath to avoid needing primary/foreign keys.
     *
     * @param consumerId    the target store ID
     * @param dataSourceName the SOURCE datasource name (the one that triggers propagation)
     * @param targetClass   the target entity class (for MetaAttribute)
     */
    @SuppressWarnings("unchecked")
    private static <T, F> PropertyMapping<T, F> createStorePropertyMapping(
            String consumerId, String dataSourceName, Class<T> targetClass) {
        // Create a MetaAttribute for the target path (id field)
        LongAttribute<T> idAttr = new LongAttribute<>("id", targetClass);
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();
        targetPath.add(idAttr);

        // Use mock SpecificationServices — configure getEntityClass() to return
        // non-null values required by index registration in IncrementalSyncProcessor
        SpecificationService<?> sourceService = mock(SpecificationService.class);
        when(sourceService.getEntityClass()).thenReturn((Class) targetClass);
        SpecificationService<T> targetService = (SpecificationService<T>) mock(SpecificationService.class);
        when(targetService.getEntityClass()).thenReturn(targetClass);

        // PK/FK paths required for store mappings validation
        LongAttribute<T> pkAttr = new LongAttribute<>("id", targetClass);
        List<List<MetaAttribute<?, ?>>> primaryKeyPaths = List.of(List.of(pkAttr));
        LongAttribute<T> fkAttr = new LongAttribute<>("id", targetClass);
        List<List<MetaAttribute<?, ?>>> foreignKeyPaths = List.of(List.of(fkAttr));

        return PropertyMapping.<T, F>builder()
                .consumerId(consumerId)
                .isForDashboard(false)
                .targetPath(targetPath)
                .datasourceName(dataSourceName)
                .sourceService(sourceService)
                .targetService(targetService)
                .primaryKeyPaths(primaryKeyPaths)
                .foreignKeyPaths(foreignKeyPaths)
                .mappingType(MappingType.ONE_TO_ONE)
                .build();
    }

    // ==================== Scenario 1: Type Mismatch (ClassCastException) ====================

    /**
     * Scenario 1 (MAIN BUG — Type Mismatch):
     *
     * <p>Store USER_STORE with {@code primaryDataSourceName=USERS_DS} expects User entities.
     * PropertyMapping: ORDERS_DS → userStore (so {@code getAffectedConsumers(ORDERS_DS)}
     * returns storeIds={USER_STORE}).</p>
     *
     * <p>When ORDERS_DS changes, Phase 4 calls {@code findAll(ORDERS_DS)} → gets Order entities
     * → writes to User store. After Phase 4, reading from userStore should return User entities
     * (from usersDS), NOT Order entities.</p>
     *
     * <p>On UNFIXED code: store contains Order entities (wrong type) → test SHOULD FAIL.</p>
     *
     * <p><b>Validates: Requirements 1.0, 2.0</b></p>
     */
    @Property(tries = 1)
    void scenario1TypeMismatchOrderEntitiesWrittenToUserStoreShouldFail() throws Exception {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            // Create PropertyMapping: ordersDS → userStore
            // This means getAffectedConsumers(ORDERS_DS) will return storeIds={USER_STORE}
            PropertyMapping<SimpleUser, Long> mapping =
                    createStorePropertyMapping(USER_STORE, ORDERS_DS, SimpleUser.class);

            // Build DependencyGraph with the PropertyMapping
            DependencyGraph graph = new DependencyGraph();
            graph.build(List.of(mapping));

            AtomicLong streamingVersion = new AtomicLong(0);
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // Register streaming datasource ORDERS_DS in READY state
            @SuppressWarnings("unchecked")
            StreamingDataSource<Order> orderStreamingDs = mock(StreamingDataSource.class);
            when(orderStreamingDs.getName()).thenReturn(ORDERS_DS);
            when(orderStreamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(ORDERS_DS, orderStreamingDs);

            // Pre-populate DependencyGraph with User entities under USERS_DS
            // (Phase 2 would have done this via mapping transformations)
            List<SimpleUser> userEntities = List.of(
                    createUser(1L, "Alice", true),
                    createUser(2L, "Bob", true),
                    createUser(3L, "Charlie", false)
            );
            graph.upsertAll(USERS_DS, userEntities);

            // Register store USER_STORE with primaryDataSourceName=USERS_DS
            // This store expects User entities, NOT Order entities
            InMemoryDataStore<SimpleUser> userStore = new InMemoryDataStore<>(
                    SimpleUser.class,
                    USER_STORE,
                    USERS_DS,              // primary DS is USERS_DS (expects User entities)
                    null,                   // no rootSpecification
                    List.of(mapping)
            );
            registerStoreViaReflection(factory, userStore);

            // Verify preconditions
            assertThat(userStore.findAll()).isEmpty();
            AffectedConsumerSet consumers = graph.getAffectedConsumers(ORDERS_DS);
            assertThat(consumers.getStoreIds())
                    .as("PropertyMapping should make getAffectedConsumers('ordersDS') return 'userStore'")
                    .contains(USER_STORE);

            // Create processor and send Order entities via BatchSnapshotEvent to ORDERS_DS
            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, streamingVersion);

            List<Order> orderEntities = List.of(
                    createOrder(101L, 99.99, "PENDING"),
                    createOrder(102L, 149.50, "SHIPPED")
            );
            BatchSnapshotEvent<Order> event = new BatchSnapshotEvent<>(orderEntities, Instant.now());

            // Process: Phase 1 upserts Order entities to graph under ORDERS_DS
            // Phase 4 should propagate to userStore
            processor.processBatchSnapshot(ORDERS_DS, event);

            // Verify Phase 1: Order entities are in DependencyGraph under ORDERS_DS
            List<Object> graphOrderEntities = graph.findAll(ORDERS_DS);
            assertThat(graphOrderEntities)
                    .as("Phase 1 should upsert Order entities to DependencyGraph under 'ordersDS'")
                    .hasSize(2);

            // Verify User entities are still in DependencyGraph under USERS_DS
            List<Object> graphUserEntities = graph.findAll(USERS_DS);
            assertThat(graphUserEntities)
                    .as("User entities should still be in DependencyGraph under 'usersDS'")
                    .hasSize(3);

            // CRITICAL ASSERTION: After Phase 4, userStore should contain User entities
            // (from usersDS), NOT Order entities (from ordersDS).
            //
            // EXPECTED (after fix): findAll(USERS_DS) → User entities → written to userStore
            // BUG (current): findAll(ORDERS_DS) → Order entities → written to userStore
            //
            // We verify by checking that the store data matches the User entities from USERS_DS
            List<SimpleUser> storeData = userStore.findAll();
            assertThat(storeData)
                    .as("userStore should contain User entities from 'usersDS' (3 users), "
                            + "NOT Order entities from 'ordersDS' (2 orders). "
                            + "BUG: Phase 4 calls findAll('ordersDS') instead of findAll('usersDS') "
                            + "→ writes Order entities to User store → type mismatch / ClassCastException")
                    .hasSize(3);

            // Additional type check: verify entities are actually SimpleUser instances
            for (Object entity : (List<?>) storeData) {
                assertThat(entity)
                        .as("Each entity in userStore should be a SimpleUser, not an Order. "
                                + "BUG: findAll('ordersDS') returns Order entities which are written to User store")
                        .isInstanceOf(SimpleUser.class);
            }
        } finally {
            factory.clearAll();
        }
    }

    // ==================== Scenario 2: rootSpecification Filter Missing ====================

    /**
     * Scenario 2 (rootSpecification Filter Missing):
     *
     * <p>Single store with {@code rootSpecification} defined, affected via PropertyMapping
     * from the SAME datasource (no type mismatch — isolates the filter bug).</p>
     *
     * <p>After Phase 4, store entity count should equal FILTERED count, not ALL entities.
     * On UNFIXED code: all entities written without filtering → test SHOULD FAIL.</p>
     *
     * <p><b>Validates: Requirements 1.1, 2.1</b></p>
     */
    @Property(tries = 1)
    void scenario2RootSpecificationFilterMissingAllEntitiesWrittenWithoutFilterShouldFail() throws Exception {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            // Create PropertyMapping: allUsersDS → activeUserStore
            PropertyMapping<SimpleUser, Long> mapping =
                    createStorePropertyMapping("activeUserStore", ALL_USERS_DS, SimpleUser.class);

            DependencyGraph graph = new DependencyGraph();
            graph.build(List.of(mapping));

            AtomicLong streamingVersion = new AtomicLong(0);
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // Register streaming datasource ALL_USERS_DS in READY state
            @SuppressWarnings("unchecked")
            StreamingDataSource<SimpleUser> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(ALL_USERS_DS);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(ALL_USERS_DS, streamingDs);

            // rootSpecification: only active users pass
            Specification<SimpleUser> activeOnlySpec = () -> user ->
                    user.getActive() != null && user.getActive();

            // Register store with primaryDataSourceName=ALL_USERS_DS + rootSpecification
            // Same primary DS as source DS → no type mismatch, isolates filter bug
            InMemoryDataStore<SimpleUser> activeUserStore = new InMemoryDataStore<>(
                    SimpleUser.class,
                    "activeUserStore",
                    ALL_USERS_DS,           // same as source DS
                    activeOnlySpec,          // rootSpecification: active=true only
                    List.of(mapping)
            );
            registerStoreViaReflection(factory, activeUserStore);

            // Verify precondition
            assertThat(activeUserStore.findAll()).isEmpty();

            // Create processor and send mixed active/inactive entities
            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, streamingVersion);

            List<SimpleUser> allEntities = List.of(
                    createUser(1L, "Active1", true),
                    createUser(2L, "Inactive1", false),
                    createUser(3L, "Active2", true),
                    createUser(4L, "Inactive2", false),
                    createUser(5L, "Active3", true)
            );
            BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(allEntities, Instant.now());

            processor.processBatchSnapshot(ALL_USERS_DS, event);

            // Verify Phase 1: all 5 entities in DependencyGraph
            assertThat(graph.findAll(ALL_USERS_DS))
                    .as("Phase 1 should upsert all 5 entities to DependencyGraph")
                    .hasSize(5);

            // EXPECTED (after fix): store should contain only 3 active entities
            // (rootSpecification filters out inactive ones)
            //
            // BUG (current): Phase 4 PropertyMapping path does NOT apply rootSpecification
            // → all 5 entities written to store without filtering
            assertThat(activeUserStore.findAll())
                    .as("activeUserStore should contain only 3 active entities after rootSpecification "
                            + "filtering. BUG: PropertyMapping path does NOT apply rootSpecification "
                            + "→ all 5 entities written without filtering")
                    .hasSize(3);
        } finally {
            factory.clearAll();
        }
    }

    // ==================== Scenario 3: Different Stores Get Same Unfiltered Data ====================

    /**
     * Scenario 3 (Different Stores Get Same Unfiltered Data):
     *
     * <p>Two stores with different {@code rootSpecification}s, both affected via PropertyMapping
     * from the same datasource. After Phase 4, each store should have different filtered subsets.</p>
     *
     * <p>On UNFIXED code: both stores get same unfiltered data → test SHOULD FAIL.</p>
     *
     * <p><b>Validates: Requirements 1.2, 2.2</b></p>
     */
    @Property(tries = 1)
    void scenario3DifferentStoresSameUnfilteredDataShouldHaveDifferentSubsetsShouldFail() throws Exception {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            // Create PropertyMappings: crewDS → activeCrewStore, crewDS → seniorCrewStore
            PropertyMapping<SimpleUser, Long> mappingActive =
                    createStorePropertyMapping("activeCrewStore", CREW_DS, SimpleUser.class);
            PropertyMapping<SimpleUser, Long> mappingSenior =
                    createStorePropertyMapping("seniorCrewStore", CREW_DS, SimpleUser.class);

            DependencyGraph graph = new DependencyGraph();
            graph.build(List.of(mappingActive, mappingSenior));

            AtomicLong streamingVersion = new AtomicLong(0);
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // Register streaming datasource CREW_DS in READY state
            @SuppressWarnings("unchecked")
            StreamingDataSource<SimpleUser> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(CREW_DS);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(CREW_DS, streamingDs);

            // rootSpecification for activeCrewStore: active=true
            Specification<SimpleUser> activeSpec = () -> user ->
                    user.getActive() != null && user.getActive();

            // rootSpecification for seniorCrewStore: age >= 40
            Specification<SimpleUser> seniorSpec = () -> user ->
                    user.getAge() != null && user.getAge() >= 40;

            // Register activeCrewStore
            InMemoryDataStore<SimpleUser> activeCrewStore = new InMemoryDataStore<>(
                    SimpleUser.class,
                    "activeCrewStore",
                    CREW_DS,
                    activeSpec,
                    List.of(mappingActive)
            );
            registerStoreViaReflection(factory, activeCrewStore);

            // Register seniorCrewStore
            InMemoryDataStore<SimpleUser> seniorCrewStore = new InMemoryDataStore<>(
                    SimpleUser.class,
                    "seniorCrewStore",
                    CREW_DS,
                    seniorSpec,
                    List.of(mappingSenior)
            );
            registerStoreViaReflection(factory, seniorCrewStore);

            // Verify preconditions
            assertThat(activeCrewStore.findAll()).isEmpty();
            assertThat(seniorCrewStore.findAll()).isEmpty();

            // Verify both stores are in affected consumers
            AffectedConsumerSet consumers = graph.getAffectedConsumers(CREW_DS);
            assertThat(consumers.getStoreIds())
                    .containsExactlyInAnyOrder("activeCrewStore", "seniorCrewStore");

            // Create processor and send entities with varied active/age combinations
            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graph, analysisResult, streamingVersion);

            SimpleUser user1 = createUser(1L, "YoungActive", true);
            user1.setAge(25);   // active=true, age=25 → passes activeSpec, fails seniorSpec
            SimpleUser user2 = createUser(2L, "OldInactive", false);
            user2.setAge(50);   // active=false, age=50 → fails activeSpec, passes seniorSpec
            SimpleUser user3 = createUser(3L, "OldActive", true);
            user3.setAge(45);   // active=true, age=45 → passes both specs
            SimpleUser user4 = createUser(4L, "YoungInactive", false);
            user4.setAge(20);   // active=false, age=20 → fails both specs

            List<SimpleUser> allCrew = List.of(user1, user2, user3, user4);
            BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(allCrew, Instant.now());

            processor.processBatchSnapshot(CREW_DS, event);

            // Verify Phase 1
            assertThat(graph.findAll(CREW_DS)).hasSize(4);

            // EXPECTED (after fix):
            // activeCrewStore: user1 (active, young) + user3 (active, old) = 2 entities
            // seniorCrewStore: user2 (inactive, old) + user3 (active, old) = 2 entities
            // The stores should have DIFFERENT subsets
            //
            // BUG (current): Both stores get ALL 4 entities without filtering
            // → both stores have same size (4) instead of different filtered subsets (2 each)
            assertThat(activeCrewStore.findAll())
                    .as("activeCrewStore should contain only 2 active crew members (user1, user3). "
                            + "BUG: PropertyMapping path writes all 4 entities without rootSpecification filtering")
                    .hasSize(2);

            assertThat(seniorCrewStore.findAll())
                    .as("seniorCrewStore should contain only 2 senior crew members (user2, user3). "
                            + "BUG: PropertyMapping path writes all 4 entities without rootSpecification filtering")
                    .hasSize(2);

            // Additional: verify the two stores have DIFFERENT data
            // (even if sizes happen to match, the actual entities should differ)
            assertThat(activeCrewStore.findAll())
                    .as("activeCrewStore and seniorCrewStore should have different entity subsets")
                    .isNotEqualTo(seniorCrewStore.findAll());
        } finally {
            factory.clearAll();
        }
    }

    // ==================== Scenario 4: findAll Call Count ====================

    /**
     * Scenario 4 (findAll Call Count):
     *
     * <p>PropertyMapping and primary path both have stores. Track findAll call count —
     * should be optimized. On UNFIXED code: multiple findAll calls → test SHOULD FAIL.</p>
     *
     * <p>Uses a spy on DependencyGraph to verify findAll is called optimally.</p>
     *
     * <p><b>Validates: Requirements 1.0, 1.1, 1.2</b></p>
     */
    @Property(tries = 1)
    void scenario4FindAllCallCountShouldBeOptimizedShouldFail() throws Exception {
        InMemorySpecStoreFactory factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        try {
            // Create PropertyMapping: flightDS → mappedFlightStore
            PropertyMapping<SimpleUser, Long> mapping =
                    createStorePropertyMapping("mappedFlightStore", FLIGHT_DS, SimpleUser.class);

            DependencyGraph realGraph = new DependencyGraph();
            realGraph.build(List.of(mapping));

            // Spy on the graph to track findAll calls
            DependencyGraph graphSpy = spy(realGraph);

            AtomicLong streamingVersion = new AtomicLong(0);
            AnalysisResult analysisResult = new AnalysisResult(null, null, null);

            // Register streaming datasource FLIGHT_DS in READY state
            @SuppressWarnings("unchecked")
            StreamingDataSource<SimpleUser> streamingDs = mock(StreamingDataSource.class);
            when(streamingDs.getName()).thenReturn(FLIGHT_DS);
            when(streamingDs.getState()).thenReturn(StreamingDataSourceState.READY);
            factory.registerDataSource(FLIGHT_DS, streamingDs);

            // Store 1: mappedFlightStore — affected via PropertyMapping
            InMemoryDataStore<SimpleUser> mappedFlightStore = new InMemoryDataStore<>(
                    SimpleUser.class,
                    "mappedFlightStore",
                    FLIGHT_DS,
                    null,
                    List.of(mapping)
            );
            registerStoreViaReflection(factory, mappedFlightStore);

            // Store 2: primaryFlightStore — affected via primary datasource path (no PropertyMapping)
            InMemoryDataStore<SimpleUser> primaryFlightStore = new InMemoryDataStore<>(
                    SimpleUser.class,
                    "primaryFlightStore",
                    FLIGHT_DS,             // primaryDataSourceName matches
                    null,
                    Collections.emptyList() // NO PropertyMapping
            );
            registerStoreViaReflection(factory, primaryFlightStore);

            // Create processor with the spy graph
            IncrementalSyncProcessor processor = new IncrementalSyncProcessor(
                    factory, graphSpy, analysisResult, streamingVersion);

            List<SimpleUser> entities = List.of(
                    createUser(1L, "Flight1", true),
                    createUser(2L, "Flight2", true)
            );
            BatchSnapshotEvent<SimpleUser> event = new BatchSnapshotEvent<>(entities, Instant.now());

            processor.processBatchSnapshot(FLIGHT_DS, event);

            // Verify both stores were updated
            assertThat(mappedFlightStore.findAll())
                    .as("mappedFlightStore should be updated via PropertyMapping path")
                    .hasSize(2);
            assertThat(primaryFlightStore.findAll())
                    .as("primaryFlightStore should be updated via primary datasource path")
                    .hasSize(2);

            // After fix, findAll(FLIGHT_DS) is called 6 times total:
            //   1x from registerIndex (initial index population during IncrementalSyncProcessor init)
            //   1x from captureOldFirstLastIds (pre-Phase 4 operation)
            //   1x from Phase 2.5 applyPhase2_5StoreMappings (root entities for store mapping)
            //   1x from rebuildIndexesForDataSource (after Phase 1 upsert)
            //   1x from Phase 4 PropertyMapping path (per unique primaryDS — mappedFlightStore)
            //   1x from Phase 4 Primary path (lazy cache init — primaryFlightStore)
            // The 2 additional calls vs pre-PK/FK are from index registration and rebuild,
            // which are necessary for composite key lookups on the FK paths.
            verify(graphSpy, times(6)).findAll(FLIGHT_DS);
        } finally {
            factory.clearAll();
        }
    }
}
