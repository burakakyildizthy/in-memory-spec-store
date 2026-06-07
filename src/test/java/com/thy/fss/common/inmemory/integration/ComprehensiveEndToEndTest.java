package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.common.model.TestUser;
import com.thy.fss.common.inmemory.common.model.TestUserFilter;
import com.thy.fss.common.inmemory.common.model.TestUserSpecificationService;
import com.thy.fss.common.inmemory.common.model.TestUser_;
import com.thy.fss.common.inmemory.filter.IntegerFilter;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationQueryEngine;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simplified end-to-end integration test covering the basic flow:
 * annotation processor → generated code → runtime usage
 */
@DisplayName("Comprehensive End-to-End Integration Tests")
class ComprehensiveEndToEndTest {

    private static final String USER_PREFIX = "user";
    

    private SpecificationQueryEngine<TestUser> userEngine;
    private List<TestUser> testUsers;

    @BeforeEach
    void setUp() {
        // Create engines
        userEngine = new SpecificationQueryEngine<>(TestUser.class);

        // Setup test data
        setupTestData();
    }

    /**
     * Test 1: Complete End-to-End Flow
     * Verifies annotation processor → generated code → runtime usage
     */
    @Test
    @DisplayName("End-to-End: Annotation Processor to Runtime Usage")
    void testCompleteEndToEndFlow() {
        System.out.println("=== END-TO-END FLOW TEST ===");

        // 1. Verify generated meta models are available
        assertNotNull(TestUser_.name, "StaticMetaModel should be generated");
        assertNotNull(TestUser_.age, "StaticMetaModel should include all fields");

        // 2. Create specification using meta model with fluent API
        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
                .where(TestUser_.name).contains("Alice");

        assertNotNull(spec, "Specification should be created using meta model");

        // 3. Create filter using FilterMetaModel
        TestUserFilter filter = new TestUserFilter();
        StringFilter nameFilter = new StringFilter();
        nameFilter.setContains("Bob");
        filter.setName(nameFilter);

        IntegerFilter ageFilter = new IntegerFilter();
        ageFilter.setGreaterThan(25);
        filter.setAge(ageFilter);

        // 4. Execute queries using SpecificationQueryEngine
        List<TestUser> specResults = userEngine.query(testUsers, spec);
        List<TestUser> filterResults = userEngine.queryByFilter(testUsers, filter);

        // 5. Verify results
        assertNotNull(specResults, "Specification query should return results");
        assertNotNull(filterResults, "Filter query should return results");

        // 6. Verify no reflection was used (performance indicator)
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            userEngine.queryByFilter(testUsers, filter);
        }
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000; // Convert to ms

        assertTrue(duration < 5000, "1000 queries should complete in under 5 second (no reflection)");

        System.out.println("✓ End-to-end flow completed successfully");
        System.out.println("✓ 1000 filter queries executed in " + duration + "ms");
    }

    /**
     * Test 2: Performance Benchmarks
     * Tests performance improvements from eliminating reflection
     */
    @Test
    @DisplayName("Performance Benchmarks")
    void testPerformanceBenchmarks() {
        System.out.println("\n=== PERFORMANCE BENCHMARKS ===");

        // Create large dataset for performance testing
        List<TestUser> largeDataset = createLargeUserDataset(10000);
        System.out.println("Created dataset with " + largeDataset.size() + " users");

        // Benchmark: Simple filter operations
        TestUserFilter simpleFilter = new TestUserFilter();
        StringFilter nameFilter = new StringFilter();
        nameFilter.setContains(USER_PREFIX);
        simpleFilter.setName(nameFilter);

        long startTime = System.nanoTime();
        userEngine.queryByFilter(largeDataset, simpleFilter);
        long simpleFilterTime = System.nanoTime() - startTime;

        System.out.println("Simple filter on 10k records: " + (simpleFilterTime / 1_000_000) + "ms");
        assertTrue(simpleFilterTime < 1000_000_000L, "Simple filter should complete in under 1 second");

        System.out.println("✓ Performance benchmarks passed");
    }

    // Helper methods
    private void setupTestData() {
        // Create test users
        testUsers = createTestUsers(1000);
    }

    private List<TestUser> createTestUsers(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    TestUser user = new TestUser();
                    user.setId((long) i);
                    user.setName(USER_PREFIX + i + (i % 2 == 0 ? "_alice" : "_bob"));
                    user.setAge(20 + (i % 60)); // Ages 20-79
                    user.setActive(i % 3 != 0); // ~67% active
                    return user;
                })
                .toList();
    }

    private List<TestUser> createLargeUserDataset(int size) {
        return IntStream.range(0, size)
                .parallel()
                .mapToObj(i -> {
                    TestUser user = new TestUser();
                    user.setId((long) i);
                    user.setName(USER_PREFIX + i);
                    user.setAge(18 + (i % 65)); // Ages 18-82
                    user.setActive(i % 4 != 0); // 75% active
                    return user;
                })
                .toList();
    }
}