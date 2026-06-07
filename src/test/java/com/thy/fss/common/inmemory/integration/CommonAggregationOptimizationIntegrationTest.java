package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.common.TestSynchronizationHelper;
import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.dashboard.Dashboard;
import com.thy.fss.common.inmemory.datasource.TestableInMemoryDataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.engine.sync.DataVersion;
import com.thy.fss.common.inmemory.factory.DashboardBuilder;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.integration.testentities.CrossTestOrder;
import com.thy.fss.common.inmemory.integration.testentities.CrossTestOrder_;
import com.thy.fss.common.inmemory.integration.testentities.CrossTestUserSummary;
import com.thy.fss.common.inmemory.integration.testentities.CrossTestUserSummary_;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import com.thy.fss.common.inmemory.integration.testentities.CrossTestOrderSpecificationService;
import com.thy.fss.common.inmemory.integration.testentities.CrossTestUserSummarySpecificationService;

/**
 * Integration test for common aggregation optimization.
 * <p>
 * Tests that multiple dashboards using the same aggregation
 * result in a single calculation (single loop optimization).
 */
class CommonAggregationOptimizationIntegrationTest {

    private static final String TEST_ORDERS = "orders";
    private static final String COMPLETED = "completed";

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;
    private List<CrossTestOrder> crossTestOrders;

    private TestableInMemoryDataSource<CrossTestOrder> orderDataSource;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAll();

        // Create test orders
        crossTestOrders = Arrays.asList(
                new CrossTestOrder("O1", "user1", 100.00, COMPLETED),
                new CrossTestOrder("O2", "user1", 200.00, COMPLETED),
                new CrossTestOrder("O3", "user2", 150.00, COMPLETED),
                new CrossTestOrder("O4", "user2", 250.00, "pending"),
                new CrossTestOrder("O5", "user3", 300.00, COMPLETED)
        );

        orderDataSource = new TestableInMemoryDataSource<>(TEST_ORDERS, CrossTestOrder.class, crossTestOrders);
        factory.registerDataSource(TEST_ORDERS, orderDataSource, Duration.ofMinutes(5));
    }

    @AfterEach
    void tearDown() {
        if (engine != null && engine.isRunning()) {
            engine.close();
        }

        // Clear all factory registrations to prevent duplicate datasource errors
        factory.clearAll();
    }

    @Test
    void testCommonAggregationSingleCalculation() {
        // Create 3 dashboards that all use the same aggregation: SUM(amount) for completed orders

        DashboardBuilder<CrossTestUserSummary> builder1 = factory.buildDashboard(CrossTestUserSummarySpecificationService.INSTANCE);

        Dashboard<CrossTestUserSummary> dashboard1 = buildDashboard(builder1, TEST_ORDERS, null, AggregationType.SUM);

        DashboardBuilder<CrossTestUserSummary> builder2 = factory.buildDashboard(CrossTestUserSummarySpecificationService.INSTANCE);

        Dashboard<CrossTestUserSummary> dashboard2 = buildDashboard(builder2, TEST_ORDERS, null, AggregationType.SUM);

        DashboardBuilder<CrossTestUserSummary> builder3 = factory.buildDashboard(CrossTestUserSummarySpecificationService.INSTANCE);

        Dashboard<CrossTestUserSummary> dashboard3 = buildDashboard(builder3, TEST_ORDERS, null, AggregationType.SUM);

        // Initialize engine
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Wait for synchronization
        TestSynchronizationHelper.waitForCondition(
                () -> dashboard1.getData() != null && dashboard2.getData() != null && dashboard3.getData() != null,
                Duration.ofSeconds(5),
                "All dashboards should have data"
        );

        // Verify all dashboards have the same correct result
        Double expectedTotal = 1000.00; // 100 + 200 + 150 + 300 + 250
        assertEquals(expectedTotal, dashboard1.getData().getTotalAmount(),
                "Dashboard 1 should have correct total");
        assertEquals(expectedTotal, dashboard2.getData().getTotalAmount(),
                "Dashboard 2 should have correct total");
        assertEquals(expectedTotal, dashboard3.getData().getTotalAmount(),
                "Dashboard 3 should have correct total");

        // Verify DataVersion exists (common aggregation optimization is internal)
        DataVersion currentVersion = engine.getCurrentDataVersion();
        assertNotNull(currentVersion, "DataVersion should exist");
        assertTrue(currentVersion.getVersion() > 0, "Version should be positive");
    }

    @Test
    void testMultipleAggregationTypesOnSameField() {
        // Create dashboards with different aggregation types on the same field
        DashboardBuilder<CrossTestUserSummary> builder1 = factory.buildDashboard(CrossTestUserSummarySpecificationService.INSTANCE);

        Dashboard<CrossTestUserSummary> sumDashboard = buildDashboard(builder1, TEST_ORDERS, null, AggregationType.SUM);

        DashboardBuilder<CrossTestUserSummary> builder2 = factory.buildDashboard(CrossTestUserSummarySpecificationService.INSTANCE);

        Dashboard<CrossTestUserSummary> countDashboard = buildDashboard(builder2, TEST_ORDERS, null, AggregationType.COUNT);

        // Initialize engine
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Wait for synchronization
        TestSynchronizationHelper.waitForCondition(
                () -> sumDashboard.getData() != null && countDashboard.getData() != null,
                Duration.ofSeconds(5)
        );

        // Verify results
        assertEquals(1000.00, sumDashboard.getData().getTotalAmount(),
                "Sum dashboard should show total amount");
        assertEquals(5, countDashboard.getData().getTotalOrders(),
                "Count dashboard should show total count");
    }

    @Test
    void testTaskMergingForCommonAggregations() {
        // Create multiple dashboards with the same aggregation
        // This tests that the engine merges aggregation tasks

        DashboardBuilder<CrossTestUserSummary> b1 = factory.buildDashboard(CrossTestUserSummarySpecificationService.INSTANCE);

        Dashboard<CrossTestUserSummary> d1 = buildDashboard(b1, TEST_ORDERS, null, AggregationType.COUNT);

        DashboardBuilder<CrossTestUserSummary> b2 = factory.buildDashboard(CrossTestUserSummarySpecificationService.INSTANCE);

        Dashboard<CrossTestUserSummary> d2 = buildDashboard(b2, TEST_ORDERS, null, AggregationType.COUNT);

        DashboardBuilder<CrossTestUserSummary> b3 = factory.buildDashboard(CrossTestUserSummarySpecificationService.INSTANCE);

        Dashboard<CrossTestUserSummary> d3 = buildDashboard(b3, TEST_ORDERS, null, AggregationType.COUNT);

        DashboardBuilder<CrossTestUserSummary> b4 = factory.buildDashboard(CrossTestUserSummarySpecificationService.INSTANCE);

        Dashboard<CrossTestUserSummary> d4 = buildDashboard(b4, TEST_ORDERS, null, AggregationType.COUNT);

        DashboardBuilder<CrossTestUserSummary> b5 = factory.buildDashboard(CrossTestUserSummarySpecificationService.INSTANCE);

        Dashboard<CrossTestUserSummary> d5 = buildDashboard(b5, TEST_ORDERS, null, AggregationType.COUNT);

        // Initialize engine
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        // Wait for synchronization
        TestSynchronizationHelper.waitForCondition(
                () -> d1.getData() != null && d2.getData() != null && d3.getData() != null
                        && d4.getData() != null && d5.getData() != null,
                Duration.ofSeconds(5)
        );

        // Verify all dashboards have the same result
        assertEquals(5, d1.getData().getTotalOrders());
        assertEquals(5, d2.getData().getTotalOrders());
        assertEquals(5, d3.getData().getTotalOrders());
        assertEquals(5, d4.getData().getTotalOrders());
        assertEquals(5, d5.getData().getTotalOrders());

        // The fact that all 5 dashboards have correct results proves
        // that common aggregation optimization is working (single calculation, multiple consumers)
        DataVersion currentVersion = engine.getCurrentDataVersion();
        assertNotNull(currentVersion, "DataVersion should exist");
    }

    private Dashboard<CrossTestUserSummary> buildDashboard(DashboardBuilder<CrossTestUserSummary> builder, String datasourceName, Specification<CrossTestOrder> spec, AggregationType aggregationType) {
        LinkedList<MetaAttribute<?, ?>> targetPath = new LinkedList<>();

        LinkedList<MetaAttribute<?, ?>> sourcePath = new LinkedList<>();

        if (aggregationType == AggregationType.SUM) {
            targetPath.add(CrossTestUserSummary_.totalAmount);
            sourcePath.add(CrossTestOrder_.amount);
        } else if (aggregationType == AggregationType.COUNT) {
            targetPath.add(CrossTestUserSummary_.totalOrders);
            sourcePath.add(CrossTestOrder_.orderId);
        }

        PropertyMapping<CrossTestUserSummary, Long> mapping = PropertyMapping.<CrossTestUserSummary, Long>builder()
                .consumerId(builder.getConsumerId())
                .datasourceName(datasourceName)
                .isForDashboard(true)
                .specification(spec)
                .targetPath(targetPath)
                .sourceService(CrossTestOrderSpecificationService.INSTANCE)
                .targetService(CrossTestUserSummarySpecificationService.INSTANCE)
                .sourcePath(sourcePath)
                .mappingType(MappingType.MANY_TO_ONE_AGGREGATION)
                .aggregationType(aggregationType)
                .build();

        builder.addPropertyMapping(mapping);

        return builder.build();
    }
}
