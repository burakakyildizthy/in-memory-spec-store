package com.thy.fss.common.inmemory.factory;

import com.thy.fss.common.inmemory.datasource.*;
import com.thy.fss.common.inmemory.entity.Identifiable;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;

import com.thy.fss.common.inmemory.specification.Specification;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for unified registration — instanceof-based streaming detection.
 *
 * Feature: streaming-datasource-unification, Property 3: Birleşik Kayıt — instanceof ile Streaming Tespiti
 *
 * Validates: Requirements 3.1, 3.2, 3.4
 */
class UnifiedRegistrationPropertyTest {

    // Feature: streaming-datasource-unification, Property 3: Birleşik Kayıt — instanceof ile Streaming Tespiti

    private InMemorySpecStoreFactory factory;

    @BeforeTry
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAllDataSources();
    }

    @AfterTry
    void tearDown() {
        factory.clearAllDataSources();
    }

    /**
     * Property 3: Birleşik Kayıt — instanceof ile Streaming Tespiti
     *
     * For any combination of batch/streaming datasources registered via registerDataSource(),
     * isStreamingDataSource(name) must return true for streaming datasources and false for batch.
     * getAllStreamingDataSourceNames() must return only streaming datasource names.
     *
     * Validates: Requirements 3.1, 3.2, 3.4
     */
    @Property(tries = 100)
    @Label("Feature: streaming-datasource-unification, Property 3: Birleşik Kayıt — instanceof ile Streaming Tespiti")
    void registeredDataSourcesShouldBeCorrectlyDetectedByType(
            @ForAll("dataSourceCombinations") List<DataSourceEntry> entries) {

        // Register all datasources
        Set<String> expectedStreamingNames = new HashSet<>();
        Set<String> expectedBatchNames = new HashSet<>();

        for (DataSourceEntry entry : entries) {
            if (entry.isStreaming) {
                factory.registerDataSource(entry.name, entry.streamingDataSource);
                expectedStreamingNames.add(entry.name);
            } else {
                factory.registerDataSource(entry.name, entry.batchDataSource, Duration.ofMinutes(5));
                expectedBatchNames.add(entry.name);
            }
        }

        // Verify isStreamingDataSource() returns correct value for each registered datasource
        for (String streamingName : expectedStreamingNames) {
            assertTrue(factory.isStreamingDataSource(streamingName),
                    "isStreamingDataSource('" + streamingName + "') should return true for streaming datasource");
        }

        for (String batchName : expectedBatchNames) {
            assertFalse(factory.isStreamingDataSource(batchName),
                    "isStreamingDataSource('" + batchName + "') should return false for batch datasource");
        }

        // Verify getAllStreamingDataSourceNames() returns exactly the streaming datasource names
        Set<String> actualStreamingNames = factory.getAllStreamingDataSourceNames();
        assertEquals(expectedStreamingNames, actualStreamingNames,
                "getAllStreamingDataSourceNames() should return exactly the streaming datasource names");
    }

    @Provide
    Arbitrary<List<DataSourceEntry>> dataSourceCombinations() {
        // Generate 1-8 datasource entries with unique names
        return Arbitraries.integers().between(1, 8).flatMap(count -> {
            // Generate unique names
            Arbitrary<List<String>> uniqueNames = Arbitraries.strings()
                    .alpha().ofMinLength(3).ofMaxLength(12)
                    .list().ofSize(count)
                    .filter(names -> names.stream().distinct().count() == names.size());

            return uniqueNames.flatMap(names -> {
                // For each name, randomly decide batch or streaming
                Arbitrary<List<Boolean>> streamingFlags = Arbitraries.of(true, false)
                        .list().ofSize(names.size());

                return streamingFlags.map(flags -> {
                    List<DataSourceEntry> entries = new ArrayList<>();
                    for (int i = 0; i < names.size(); i++) {
                        entries.add(new DataSourceEntry(names.get(i), flags.get(i)));
                    }
                    return entries;
                });
            });
        });
    }

    // --- Data structures ---

    // Feature: streaming-datasource-unification, Property 4: TimeWindowRule Kayıt Round-Trip

    /**
     * Property 4: TimeWindowRule Kayıt Round-Trip
     *
     * For any StreamingDataSource and associated TimeWindowRule, after registering the datasource
     * via registerDataSource() and associating the TimeWindowRule via registerTimeWindowRule(),
     * getTimeWindowRule(name) must return the same TimeWindowRule object (identity check).
     *
     * Validates: Requirements 3.3
     */
    @Property(tries = 100)
    @Label("Feature: streaming-datasource-unification, Property 4: TimeWindowRule Kayıt Round-Trip")
    void registeredTimeWindowRuleShouldBeRetrievableByName(
            @ForAll("timeWindowRuleEntries") List<TimeWindowRuleEntry> entries) {

        // Register all streaming datasources and their TimeWindowRules
        for (TimeWindowRuleEntry entry : entries) {
            factory.registerDataSource(entry.dsName, entry.streamingDataSource);
            factory.registerTimeWindowRule(entry.dsName, entry.timeWindowRule);
        }

        // Verify getTimeWindowRule() returns the exact same object for each
        for (TimeWindowRuleEntry entry : entries) {
            TimeWindowRule<?> retrieved = factory.getTimeWindowRule(entry.dsName);
            assertNotNull(retrieved,
                    "getTimeWindowRule('" + entry.dsName + "') should not return null after registration");
            assertSame(entry.timeWindowRule, retrieved,
                    "getTimeWindowRule('" + entry.dsName + "') should return the same object that was registered");
        }
    }

    @Provide
    Arbitrary<List<TimeWindowRuleEntry>> timeWindowRuleEntries() {
        return Arbitraries.integers().between(1, 8).flatMap(count -> {
            Arbitrary<List<String>> uniqueNames = Arbitraries.strings()
                    .alpha().ofMinLength(3).ofMaxLength(12)
                    .list().ofSize(count)
                    .filter(names -> names.stream().distinct().count() == names.size());

            return uniqueNames.map(names -> {
                List<TimeWindowRuleEntry> entries = new ArrayList<>();
                for (String name : names) {
                    entries.add(new TimeWindowRuleEntry(name));
                }
                return entries;
            });
        });
    }

    static class TimeWindowRuleEntry {
        final String dsName;
        final StubStreamingDataSource streamingDataSource;
        final TimeWindowRule<TestEntity> timeWindowRule;

        TimeWindowRuleEntry(String dsName) {
            this.dsName = dsName;
            this.streamingDataSource = new StubStreamingDataSource(dsName);
            Supplier<Specification<TestEntity>> specFactory = () -> new Specification<>() {
                @Override
                public Predicate<TestEntity> toPredicate() {
                    return entity -> true;
                }
            };
            this.timeWindowRule = new TimeWindowRule<>(dsName, specFactory);
        }
    }

    // --- Data structures ---

    static class DataSourceEntry {
        final String name;
        final boolean isStreaming;
        final StubStreamingDataSource streamingDataSource;
        final StubBatchDataSource batchDataSource;

        DataSourceEntry(String name, boolean isStreaming) {
            this.name = name;
            this.isStreaming = isStreaming;
            this.streamingDataSource = isStreaming ? new StubStreamingDataSource(name) : null;
            this.batchDataSource = isStreaming ? null : new StubBatchDataSource(name);
        }
    }

    // --- Test doubles ---

    static class TestEntity implements Identifiable<Integer> {
        private final int id;

        TestEntity(int id) {
            this.id = id;
        }

        @Override
        public Integer getIdentity() {
            return id;
        }
    }

    static class StubBatchDataSource implements DataSource<TestEntity> {
        private final String name;

        StubBatchDataSource(String name) {
            this.name = name;
        }

        @Override
        public String getName() { return name; }

        @Override
        public Class<TestEntity> getEntityType() { return TestEntity.class; }

        @Override
        public CompletableFuture<List<TestEntity>> fetchAll() {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public CompletableFuture<List<TestEntity>> fetchAllById(Collection<Object> ids) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public boolean isHealthy() { return true; }

        @Override
        public void close() { }

        @Override
        public Optional<DataSource<TestEntity>> getFallbackDataSource() {
            return Optional.empty();
        }

        @Override
        public void setFallbackDataSource(DataSource<TestEntity> fallbackDataSource) { }
    }

    static class StubStreamingDataSource implements StreamingDataSource<TestEntity> {
        private final String name;

        StubStreamingDataSource(String name) {
            this.name = name;
        }

        @Override
        public String getName() { return name; }

        @Override
        public Class<TestEntity> getEntityType() { return TestEntity.class; }

        @Override
        public CompletableFuture<List<TestEntity>> fetchAll() {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public CompletableFuture<List<TestEntity>> fetchAllById(Collection<Object> ids) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public boolean isHealthy() { return true; }

        @Override
        public void close() { }

        @Override
        public Optional<DataSource<TestEntity>> getFallbackDataSource() {
            return Optional.empty();
        }

        @Override
        public void setFallbackDataSource(DataSource<TestEntity> fallbackDataSource) { }

        @Override
        public StreamingDataSourceState getState() { return StreamingDataSourceState.READY; }

        @Override
        public void subscribe(BatchSnapshotEventListener<TestEntity> listener) { }

        @Override
        public void unsubscribe(BatchSnapshotEventListener<TestEntity> listener) { }
    }
}
