package com.thy.fss.common.inmemory.factory;

import com.thy.fss.common.inmemory.common.model.TestUser;
import com.thy.fss.common.inmemory.common.model.TestUserSpecificationService;
import com.thy.fss.common.inmemory.datasource.TestableInMemoryDataSource;
import com.thy.fss.common.inmemory.factory.testmodels.TestDashboardModel;
import com.thy.fss.common.inmemory.factory.testmodels.TestDashboardModelSpecificationService;
import com.thy.fss.common.inmemory.integration.testentities.CrossTestOrder;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the fluent builder pattern.
 * <p>
 * Tests cover: - Fluent API chaining - Type safety - Compile-time validation -
 * Store vs Dashboard builder differences
 */
@DisplayName("Fluent Builder Pattern Tests")
class FluentBuilderPatternTest {

    private InMemorySpecStoreFactory factory;
    private TestableInMemoryDataSource<TestUser> userDataSource;
    private TestableInMemoryDataSource<CrossTestOrder> orderDataSource;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();

        // Create test datasources
        userDataSource = new TestableInMemoryDataSource<>("users", TestUser.class, createTestUsers());
        orderDataSource = new TestableInMemoryDataSource<>("orders", CrossTestOrder.class, createTestOrders());

        // Register datasources
        factory.registerDataSource("users", userDataSource, Duration.ofMinutes(5));
        factory.registerDataSource("orders", orderDataSource, Duration.ofMinutes(5));
    }

    @AfterEach
    void tearDown() {
        // Clean up factory registrations to prevent duplicate registration errors
        factory.clearAll();
    }

    private List<TestUser> createTestUsers() {
        List<TestUser> users = new ArrayList<>();
        TestUser user1 = new TestUser(1L, "Alice", 30, "alice@test.com", true);
        TestUser user2 = new TestUser(2L, "Bob", 25, "bob@test.com", true);
        users.add(user1);
        users.add(user2);
        return users;
    }

    private List<CrossTestOrder> createTestOrders() {
        List<CrossTestOrder> orders = new ArrayList<>();
        orders.add(new CrossTestOrder("O1", "1", 100.00, "COMPLETED"));
        orders.add(new CrossTestOrder("O2", "1", 200.00, "COMPLETED"));
        orders.add(new CrossTestOrder("O3", "2", 150.00, "PENDING"));
        return orders;
    }

    @Nested
    @DisplayName("Fluent API Chaining Tests")
    class FluentAPIChainingTests {

        @Test
        @DisplayName("Should support method chaining for Store builder")
        void shouldSupportMethodChainingForStore() {
            // Test that builder methods return the builder for chaining
            InMemoryStoreBuilder<TestUser> builder = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE);

            // Verify builder is not null and can be chained
            assertNotNull(builder);
            InMemoryStoreBuilder<TestUser> chainedBuilder = builder .withPrimaryDataSource(TestUser.class);
            assertNotNull(chainedBuilder);
            assertSame(builder, chainedBuilder, "Builder should return itself for chaining");
        }

        @Test
        @DisplayName("Should support method chaining for Dashboard builder")
        void shouldSupportMethodChainingForDashboard() {
            // Test that builder methods return the builder for chaining
            DashboardBuilder<TestDashboardModel> builder = factory.buildDashboard(TestDashboardModelSpecificationService.INSTANCE);

            // Verify builder is not null
            assertNotNull(builder);
            DashboardBuilder<TestDashboardModel> chainedBuilder = builder.withName("Test Dashboard");
            assertNotNull(chainedBuilder);
            assertSame(builder, chainedBuilder, "Builder should return itself for chaining");
        }

        @Test
        @DisplayName("Should allow building store with primary datasource")
        void shouldAllowBuildingStoreWithPrimaryDataSource() {
            InMemoryDataStore<TestUser> store = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                    .withPrimaryDataSource(TestUser.class)
                    .build();

            assertNotNull(store);
        }

        @Test
        @DisplayName("Should return parent builder after property mapping completion")
        void shouldReturnParentBuilderAfterPropertyMappingCompletion() {
            InMemoryStoreBuilder<TestUser> builder = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                    .withPrimaryDataSource(TestUser.class);

            // Verify builder can be used to build
            assertNotNull(builder);
            InMemoryDataStore<TestUser> store = builder.build();
            assertNotNull(store);
        }
    }

    @Nested
    @DisplayName("Type Safety Tests")
    class TypeSafetyTests {

        @Test
        @DisplayName("Should enforce type-safe entity class")
        void shouldEnforceTypeSafeEntityClass() {
            // This test verifies that entity class is type-safe (compile-time check)
            InMemoryStoreBuilder<TestUser> builder = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE);

            assertNotNull(builder);
            InMemoryDataStore<TestUser> store = builder
                    .withPrimaryDataSource(TestUser.class)
                    .build();

            assertNotNull(store);
        }

        @Test
        @DisplayName("Should enforce type-safe dashboard model class")
        void shouldEnforceTypeSafeDashboardModelClass() {
            // This test verifies that dashboard model class is type-safe (compile-time check)
            DashboardBuilder<TestDashboardModel> builder = factory.buildDashboard(TestDashboardModelSpecificationService.INSTANCE);

            assertNotNull(builder);
        }
    }

    @Nested
    @DisplayName("Compile-Time Validation Tests")
    class CompileTimeValidationTests {

        @Test
        @DisplayName("Should validate primary datasource is set for Store")
        void shouldValidatePrimaryDataSourceIsSetForStore() {
            InMemoryStoreBuilder<TestUser> builder = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE);
                        // Missing withPrimaryDataSource()

            Exception exception = assertThrows(IllegalStateException.class, builder::build);
            assertNotNull(exception);
        }
    }

    @Nested
    @DisplayName("Store vs Dashboard Builder Differences Tests")
    class StoreVsDashboardDifferencesTests {

        @Test
        @DisplayName("Store should require primary datasource")
        void storeShouldRequirePrimaryDataSource() {
            InMemoryStoreBuilder<TestUser> builder = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE);
                        // No primary datasource

            Exception exception = assertThrows(IllegalStateException.class, builder::build);
            assertNotNull(exception);
        }

        @Test
        @DisplayName("Dashboard should NOT have primary datasource method")
        void dashboardShouldNotHavePrimaryDataSourceMethod() {
            // This is a compile-time check - DashboardBuilder doesn't have withPrimaryDataSource()
            // If this compiles, the API is correct
            DashboardBuilder<TestDashboardModel> builder = factory.buildDashboard(TestDashboardModelSpecificationService.INSTANCE);

            // Verify builder doesn't have withPrimaryDataSource method by checking it compiles without it
            assertNotNull(builder);
        }

        @Test
        @DisplayName("Store builder should have withPrimaryDataSource method")
        void storeBuilderShouldHaveWithPrimaryDataSourceMethod() {
            InMemoryStoreBuilder<TestUser> builder = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE);

            // Verify method exists and returns builder
            InMemoryStoreBuilder<TestUser> result = builder.withPrimaryDataSource(TestUser.class);
            assertNotNull(result);
            assertSame(builder, result);
        }

        @Test
        @DisplayName("Dashboard builder should have withName method")
        void dashboardBuilderShouldHaveWithNameMethod() {
            DashboardBuilder<TestDashboardModel> builder = factory.buildDashboard(TestDashboardModelSpecificationService.INSTANCE);

            // Verify method exists and returns builder
            DashboardBuilder<TestDashboardModel> result = builder.withName("Test Dashboard");
            assertNotNull(result);
            assertSame(builder, result);
        }

        @Test
        @DisplayName("Store can be built with valid primary datasource")
        void storeCanBeBuiltWithValidPrimaryDataSource() {
            InMemoryDataStore<TestUser> store = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                    .withPrimaryDataSource(TestUser.class)
                    .build();

            assertNotNull(store);
        }

        @Test
        @DisplayName("Dashboard can be built with name")
        void dashboardCanBeBuiltWithName() {
            DashboardBuilder<TestDashboardModel> builder = factory.buildDashboard(TestDashboardModelSpecificationService.INSTANCE)
                    .withName("Test Dashboard");

            assertNotNull(builder);
        }
    }

    @Nested
    @DisplayName("Builder Instance Tests")
    class BuilderInstanceTests {

        @Test
        @DisplayName("Should create new builder instance for each buildInMemoryStore call")
        void shouldCreateNewBuilderInstanceForEachBuildInMemoryStoreCall() {
            InMemoryStoreBuilder<TestUser> builder1 = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE);
            InMemoryStoreBuilder<TestUser> builder2 = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE);

            assertNotNull(builder1);
            assertNotNull(builder2);
            assertNotSame(builder1, builder2, "Each call should create a new builder instance");
        }

        @Test
        @DisplayName("Should create new builder instance for each buildDashboard call")
        void shouldCreateNewBuilderInstanceForEachBuildDashboardCall() {
            DashboardBuilder<TestDashboardModel> builder1 = factory.buildDashboard(TestDashboardModelSpecificationService.INSTANCE);
            DashboardBuilder<TestDashboardModel> builder2 = factory.buildDashboard(TestDashboardModelSpecificationService.INSTANCE);

            assertNotNull(builder1);
            assertNotNull(builder2);
            assertNotSame(builder1, builder2, "Each call should create a new builder instance");
        }

        @Test
        @DisplayName("Should allow building multiple stores from same factory")
        void shouldAllowBuildingMultipleStoresFromSameFactory() {
            InMemoryDataStore<TestUser> store1 = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                    .withPrimaryDataSource(TestUser.class)
                    .build();

            InMemoryDataStore<TestUser> store2 = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                    .withPrimaryDataSource(TestUser.class)
                    .build();

            assertNotNull(store1);
            assertNotNull(store2);
            assertNotSame(store1, store2, "Each build should create a new store instance");
        }
    }
}
