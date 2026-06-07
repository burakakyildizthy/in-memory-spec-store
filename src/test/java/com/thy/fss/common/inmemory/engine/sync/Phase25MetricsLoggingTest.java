package com.thy.fss.common.inmemory.engine.sync;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;

import com.thy.fss.common.inmemory.engine.analysis.AnalysisResult;
import com.thy.fss.common.inmemory.engine.mapping.MappingType;
import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;
import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;

/**
 * Unit tests for Phase 2.5 metrics logging in {@link IncrementalSyncProcessor}.
 *
 * <p>Verifies that the single-line summary log produced by
 * {@code applyPhase2_5StoreMappings()} contains all required sub-step metrics.</p>
 *
 * <p>Uses a custom {@link CapturingLogger} that extends SLF4J's {@link AbstractLogger}
 * to capture formatted log messages without requiring a logging backend.</p>
 *
 * <p>Validates: Requirements 5.1, 5.2, 5.3, 5.4</p>
 */
class Phase25MetricsLoggingTest {

    private static final String STORE_A = "store-A";
    private static final String DS_PRIMARY = "ds-primary";
    private static final String DS_FOREIGN = "ds-foreign";

    // ==================== Reflection Setup ====================

    private static final Method APPLY_PHASE25_METHOD;
    private static final Field LOGGER_FIELD;

    static {
        try {
            APPLY_PHASE25_METHOD = IncrementalSyncProcessor.class.getDeclaredMethod(
                    "applyPhase2_5StoreMappings", String.class, Set.class, List.class);
            APPLY_PHASE25_METHOD.setAccessible(true);

            LOGGER_FIELD = IncrementalSyncProcessor.class.getDeclaredField("logger");
            LOGGER_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up reflection", e);
        }
    }

    // ==================== Capturing Logger ====================

    /**
     * A minimal SLF4J Logger that captures formatted log messages.
     * Extends AbstractLogger so all info/debug/warn overloads are handled uniformly.
     */
    static class CapturingLogger extends AbstractLogger {
        final List<String> infoMessages = new ArrayList<>();
        final List<String> allMessages = new ArrayList<>();

        CapturingLogger() {
            this.name = "CapturingLogger";
        }

        @Override
        protected String getFullyQualifiedCallerName() {
            return null;
        }

        @Override
        protected void handleNormalizedLoggingCall(Level level, Marker marker,
                                                    String messagePattern, Object[] arguments,
                                                    Throwable throwable) {
            String formatted = formatSlf4j(messagePattern, arguments);
            allMessages.add(formatted);
            if (level == Level.INFO) {
                infoMessages.add(formatted);
            }
        }

        @Override
        public boolean isTraceEnabled() { return false; }

        @Override
        public boolean isTraceEnabled(Marker marker) { return false; }

        @Override
        public boolean isDebugEnabled() { return true; }

        @Override
        public boolean isDebugEnabled(Marker marker) { return true; }

        @Override
        public boolean isInfoEnabled() { return true; }

        @Override
        public boolean isInfoEnabled(Marker marker) { return true; }

        @Override
        public boolean isWarnEnabled() { return true; }

        @Override
        public boolean isWarnEnabled(Marker marker) { return true; }

        @Override
        public boolean isErrorEnabled() { return true; }

        @Override
        public boolean isErrorEnabled(Marker marker) { return true; }

        private static String formatSlf4j(String format, Object[] args) {
            if (format == null) return "";
            if (args == null || args.length == 0) return format;
            StringBuilder sb = new StringBuilder();
            int argIdx = 0;
            for (int i = 0; i < format.length(); i++) {
                if (i + 1 < format.length() && format.charAt(i) == '{' && format.charAt(i + 1) == '}') {
                    sb.append(argIdx < args.length ? args[argIdx++] : "{}");
                    i++; // skip '}'
                } else {
                    sb.append(format.charAt(i));
                }
            }
            return sb.toString();
        }
    }

    // ==================== Test Entity ====================

    static class TestEntity implements Identifiable<Long> {
        private final Long id;

        TestEntity(Long id) {
            this.id = id;
        }

        @Override
        public Long getIdentity() {
            return id;
        }
    }

    // ==================== Shared Fields ====================

    private IncrementalSyncProcessor processor;
    private CapturingLogger capturingLogger;
    private Logger originalLogger;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() throws Exception {
        InMemorySpecStoreFactory factory = mock(InMemorySpecStoreFactory.class);
        DependencyGraph dependencyGraph = mock(DependencyGraph.class);

        when(factory.getAllStoreIds()).thenReturn(List.of(STORE_A));

        InMemoryDataStore store = mock(InMemoryDataStore.class);
        when(store.getPrimaryDataSourceName()).thenReturn(DS_PRIMARY);
        when(factory.getStoreById(STORE_A)).thenReturn(store);

        PropertyMapping mapping = mock(PropertyMapping.class);
        when(mapping.getConsumerId()).thenReturn(STORE_A);
        when(mapping.getDataSourceName()).thenReturn(DS_FOREIGN);
        when(mapping.isForDashboard()).thenReturn(false);
        when(mapping.getMappingType()).thenReturn(MappingType.ONE_TO_ONE);
        when(mapping.getForeignKeyPaths()).thenReturn(null);
        when(mapping.getPrimaryKeyPaths()).thenReturn(null);

        when(dependencyGraph.getMappingsByConsumerId(STORE_A))
                .thenReturn(List.of(mapping));

        List<Object> rootEntities = new ArrayList<>();
        rootEntities.add(new TestEntity(1L));
        rootEntities.add(new TestEntity(2L));
        rootEntities.add(new TestEntity(3L));
        when(dependencyGraph.findAll(DS_PRIMARY)).thenReturn(rootEntities);

        when(dependencyGraph.updateIndexesForEntities(eq(DS_PRIMARY), any()))
                .thenReturn(true);

        AnalysisResult analysisResult = new AnalysisResult(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());

        processor = new IncrementalSyncProcessor(factory, dependencyGraph, analysisResult, new AtomicLong(0));

        // Replace static logger with capturing logger
        originalLogger = (Logger) LOGGER_FIELD.get(null);
        capturingLogger = new CapturingLogger();
        setStaticFinalField(LOGGER_FIELD, capturingLogger);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (originalLogger != null) {
            setStaticFinalField(LOGGER_FIELD, originalLogger);
        }
    }

    private static void setStaticFinalField(Field field, Object value) throws Exception {
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, value);
        } catch (NoSuchFieldException e) {
            var unsafeClass = Class.forName("sun.misc.Unsafe");
            var unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            var unsafe = unsafeField.get(null);

            long offset = (long) unsafeClass.getMethod("staticFieldOffset", Field.class)
                    .invoke(unsafe, field);
            Object base = unsafeClass.getMethod("staticFieldBase", Field.class)
                    .invoke(unsafe, field);
            unsafeClass.getMethod("putObject", Object.class, long.class, Object.class)
                    .invoke(unsafe, base, offset, value);
        }
    }

    // ==================== Helpers ====================

    private String findPhase25Log() {
        return capturingLogger.allMessages.stream()
                .filter(msg -> msg.startsWith("Phase2.5 ds="))
                .findFirst()
                .orElse(null);
    }

    // ==================== Tests ====================

    /**
     * Verifies that the Phase 2.5 summary log contains all sub-step timing metrics.
     *
     * <p>Validates: Requirements 5.1, 5.2, 5.3, 5.4</p>
     */
    @Test
    void logContainsAllSubStepMetrics() throws Exception {
        List<TestEntity> eventEntities = List.of(new TestEntity(1L), new TestEntity(2L));

        APPLY_PHASE25_METHOD.invoke(processor, DS_PRIMARY, new LinkedHashSet<>(), eventEntities);

        String log = findPhase25Log();

        assertThat(log)
                .as("Phase2.5 summary log should be present")
                .isNotNull();

        // Requirement 5.1: Sub-step timings
        assertThat(log).contains("mappingCollect=");
        assertThat(log).contains("entityFilter=");
        assertThat(log).contains("mappingApply=");
        assertThat(log).contains("indexUpdate=");

        // Requirement 5.2: Total time, processed root count, mapping count
        assertThat(log).contains("total=");
        assertThat(log).contains("processed=");
        assertThat(log).contains("roots");
        assertThat(log).contains("mappings=");

        // Requirement 5.3: Skip count
        assertThat(log).contains("skipped=");

        // Requirement 5.4: Cache hit rate
        assertThat(log).contains("cacheHitRate=");
    }

    /**
     * Verifies that the datasource name appears in the log.
     *
     * <p>Validates: Requirement 5.2</p>
     */
    @Test
    void logContainsDatasourceName() throws Exception {
        List<TestEntity> eventEntities = List.of(new TestEntity(1L));

        APPLY_PHASE25_METHOD.invoke(processor, DS_PRIMARY, new LinkedHashSet<>(), eventEntities);

        String log = findPhase25Log();

        assertThat(log).isNotNull();
        assertThat(log).contains("Phase2.5 ds=ds-primary");
    }

    /**
     * Verifies that skip count and cache hit rate values are correct.
     *
     * <p>With 3 root entities and 2 affected, skipped=1.
     * Cache hit rate should be a percentage value.</p>
     *
     * <p>Validates: Requirements 5.3, 5.4</p>
     */
    @Test
    void logReflectsCorrectSkipCountAndCacheHitRate() throws Exception {
        List<TestEntity> eventEntities = List.of(new TestEntity(1L), new TestEntity(2L));

        APPLY_PHASE25_METHOD.invoke(processor, DS_PRIMARY, new LinkedHashSet<>(), eventEntities);

        String log = findPhase25Log();

        assertThat(log).isNotNull().contains("processed=2/3 roots (skipped=1)").containsPattern("cacheHitRate=\\d+[.,]\\d+%");
    }
}
