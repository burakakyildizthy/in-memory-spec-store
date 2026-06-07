package com.thy.fss.common.inmemory.exception;

import com.thy.fss.common.inmemory.dashboard.exception.DashboardNotFoundException;
import com.thy.fss.common.inmemory.processor.exception.ProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for exception propagation across different components.
 * Tests how exceptions flow between layers and are properly wrapped/unwrapped.
 */
@DisplayName("Exception Propagation Tests")
class ExceptionPropagationTest {
    //burda kaldım

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("DataSource to Store Exception Propagation")
    class DataSourceToStoreTest {

        @Test
        @DisplayName("Should propagate DataSourceException as InMemoryDataStoreException")
        void testDataSourceExceptionPropagation() {
            // Simulate DataSource throwing exception
            DataSourceException originalException = new DataSourceException("Connection failed");

            // Simulate Store wrapping the exception
            InMemoryDataStoreException wrappedException = new InMemoryDataStoreException(
                    "Store operation failed", originalException);

            assertEquals("Store operation failed", wrappedException.getMessage());
            assertEquals(originalException, wrappedException.getCause());
            assertInstanceOf(DataSourceException.class, wrappedException.getCause());
        }

        @Test
        @DisplayName("Should propagate DataSourceConnectionException with context")
        void testDataSourceConnectionExceptionPropagation() {
            // Simulate connection failure
            DataSourceConnectionException connectionException = new DataSourceConnectionException(
                    "Connection timeout", "primary-db");

            // Simulate Store adding context
            SynchronizationException syncException = new SynchronizationException(
                    "Synchronization failed due to connection issue", connectionException, 123L, "FETCH");

            assertEquals("Synchronization failed due to connection issue", syncException.getMessage());
            assertEquals(connectionException, syncException.getCause());
            assertEquals(123L, syncException.getCurrentVersion());
            assertEquals("FETCH", syncException.getSyncPhase());
            assertEquals("primary-db", ((DataSourceConnectionException) syncException.getCause()).getDataSourceName());
        }
    }

    @Nested
    @DisplayName("Store to Dashboard Exception Propagation")
    class StoreToDashboardTest {

        @Test
        @DisplayName("Should propagate SynchronizationException to Dashboard")
        void testSynchronizationExceptionPropagation() {
            // Simulate Store synchronization failure
            SynchronizationException syncException = new SynchronizationException(
                    "Version conflict detected", 456L, "COMMIT");

            // Simulate Dashboard handling the exception
            RuntimeException dashboardException = new RuntimeException(
                    "Dashboard update failed: " + syncException.getMessage(), syncException);

            assertTrue(dashboardException.getMessage().contains("Dashboard update failed"));
            assertTrue(dashboardException.getMessage().contains("Version conflict detected"));
            assertEquals(syncException, dashboardException.getCause());
            assertEquals(456L, ((SynchronizationException) dashboardException.getCause()).getCurrentVersion());
        }

        @Test
        @DisplayName("Should propagate ObjectBuildingException with type information")
        void testObjectBuildingExceptionPropagation() {
            // Simulate object building failure
            ObjectBuildingException buildingException = new ObjectBuildingException(
                    "Failed to map property", String.class, "name");

            // Simulate Dashboard wrapping with context
            RuntimeException dashboardException = new RuntimeException(
                    "Dashboard data processing failed", buildingException);

            assertEquals("Dashboard data processing failed", dashboardException.getMessage());
            assertEquals(buildingException, dashboardException.getCause());

            ObjectBuildingException originalException = (ObjectBuildingException) dashboardException.getCause();
            assertEquals(String.class, originalException.getTargetClass());
            assertEquals("name", originalException.getPropertyName());
        }
    }

    @Nested
    @DisplayName("Processor Exception Propagation")
    class ProcessorExceptionTest {

        @Test
        @DisplayName("Should propagate ProcessingException during compilation")
        void testProcessingExceptionPropagation() {
            // Simulate annotation processing failure
            ProcessingException processingException = new ProcessingException(
                    "Failed to generate meta model", new IllegalStateException("Invalid annotation"));

            // Simulate compilation wrapper
            RuntimeException compilationException = new RuntimeException(
                    "Compilation failed", processingException);

            assertEquals("Compilation failed", compilationException.getMessage());
            assertEquals(processingException, compilationException.getCause());
            assertInstanceOf(IllegalStateException.class, processingException.getCause());
        }

        @Test
        @DisplayName("Should handle ProcessingException in async context")
        void testProcessingExceptionInAsyncContext() {
            ProcessingException processingException = new ProcessingException("Async processing failed");

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                throw new RuntimeException(processingException);
            });

            ExecutionException executionException = assertThrows(ExecutionException.class, future::get);
            assertInstanceOf(RuntimeException.class, executionException.getCause());
            assertEquals(processingException, executionException.getCause().getCause());
        }
    }

    @Nested
    @DisplayName("Dashboard Exception Propagation")
    class DashboardExceptionTest {

        @Test
        @DisplayName("Should propagate DashboardNotFoundException with ID context")
        void testDashboardNotFoundExceptionPropagation() {
            // Simulate dashboard lookup failure
            DashboardNotFoundException notFoundException = new DashboardNotFoundException(
                    "dashboard-123", "Dashboard not found in registry");

            // Simulate service layer wrapping
            RuntimeException serviceException = new RuntimeException(
                    "Service operation failed", notFoundException);

            assertEquals("Service operation failed", serviceException.getMessage());
            assertEquals(notFoundException, serviceException.getCause());
            assertEquals("dashboard-123", ((DashboardNotFoundException) serviceException.getCause()).getDashboardId());
        }

        @Test
        @DisplayName("Should handle nested dashboard exceptions")
        void testNestedDashboardExceptions() {
            // Create nested exception chain
            DataSourceException dataSourceException = new DataSourceException("Database error");
            SynchronizationException syncException = new SynchronizationException(
                    "Sync failed", dataSourceException, 789L);
            DashboardNotFoundException dashboardException = new DashboardNotFoundException(
                    "dash-nested", "Dashboard lookup failed during sync", syncException);

            assertEquals("dash-nested", dashboardException.getDashboardId());
            assertEquals(syncException, dashboardException.getCause());
            assertEquals(dataSourceException, dashboardException.getCause().getCause());
            assertEquals(789L, ((SynchronizationException) dashboardException.getCause()).getCurrentVersion());
        }
    }

    @Nested
    @DisplayName("Cross-Component Exception Chains")
    class CrossComponentTest {

        @Test
        @DisplayName("Should handle complete exception chain across all components")
        void testCompleteExceptionChain() {
            // Build complete exception chain: DataSource -> Store -> Dashboard -> Service
            DataSourceConnectionException dataSourceException = new DataSourceConnectionException(
                    "Network timeout", "backup-db");

            ObjectBuildingException buildingException = new ObjectBuildingException(
                    "Object mapping failed", dataSourceException, String.class, "id");

            SynchronizationException syncException = new SynchronizationException(
                    "Synchronization interrupted", buildingException, 999L, "VALIDATION");

            DashboardNotFoundException dashboardException = new DashboardNotFoundException(
                    "main-dashboard", "Dashboard unavailable", syncException);

            RuntimeException serviceException = new RuntimeException(
                    "Service request failed", dashboardException);

            // Verify complete chain
            assertEquals("Service request failed", serviceException.getMessage());
            assertEquals(dashboardException, serviceException.getCause());
            assertEquals("main-dashboard", ((DashboardNotFoundException) serviceException.getCause()).getDashboardId());

            assertEquals(syncException, serviceException.getCause().getCause());
            assertEquals(999L, ((SynchronizationException) serviceException.getCause().getCause()).getCurrentVersion());

            assertEquals(buildingException, serviceException.getCause().getCause().getCause());
            assertEquals(String.class, ((ObjectBuildingException) serviceException.getCause().getCause().getCause()).getTargetClass());

            assertEquals(dataSourceException, serviceException.getCause().getCause().getCause().getCause());
            assertEquals("backup-db", ((DataSourceConnectionException) serviceException.getCause().getCause().getCause().getCause()).getDataSourceName());
        }

        @Test
        @DisplayName("Should preserve exception information through async operations")
        void testAsyncExceptionPropagation() {
            SynchronizationException originalException = new SynchronizationException(
                    "Async sync failed", 555L, "ASYNC_COMMIT");

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                throw new RuntimeException("Async operation failed", originalException);
            });

            ExecutionException executionException = assertThrows(ExecutionException.class, future::get);
            RuntimeException runtimeException = (RuntimeException) executionException.getCause();
            SynchronizationException syncException = (SynchronizationException) runtimeException.getCause();

            assertEquals("Async sync failed", syncException.getMessage());
            assertEquals(555L, syncException.getCurrentVersion());
            assertEquals("ASYNC_COMMIT", syncException.getSyncPhase());
        }
    }

    @Nested
    @DisplayName("Exception Unwrapping Tests")
    class ExceptionUnwrappingTest {

        @Test
        @DisplayName("Should unwrap nested exceptions to find root cause")
        void testExceptionUnwrapping() {
            // Create nested exception chain
            IllegalArgumentException rootCause = new IllegalArgumentException("Invalid parameter");
            DataSourceException dataSourceException = new DataSourceException("DataSource error", rootCause);
            InMemoryDataStoreException storeException = new InMemoryDataStoreException("Store error", dataSourceException);
            RuntimeException wrapperException = new RuntimeException("Wrapper error", storeException);

            // Unwrap to find root cause
            Throwable current = wrapperException;
            while (current.getCause() != null) {
                current = current.getCause();
            }

            assertEquals(rootCause, current);
            assertEquals("Invalid parameter", current.getMessage());
            assertInstanceOf(IllegalArgumentException.class, current);
        }

        @Test
        @DisplayName("Should find specific exception type in chain")
        void testFindSpecificExceptionType() {
            // Create chain with specific exception type
            DataSourceConnectionException connectionException = new DataSourceConnectionException(
                    "Connection lost", "test-db");
            SynchronizationException syncException = new SynchronizationException(
                    "Sync failed", connectionException, 123L);
            RuntimeException wrapperException = new RuntimeException("Operation failed", syncException);

            // Find DataSourceConnectionException in chain
            DataSourceConnectionException foundException = findExceptionInChain(
                    wrapperException, DataSourceConnectionException.class);

            assertNotNull(foundException);
            assertEquals("Connection lost", foundException.getMessage());
            assertEquals("test-db", foundException.getDataSourceName());
        }

        @SuppressWarnings("unchecked")
        private <T extends Throwable> T findExceptionInChain(Throwable exception, Class<T> targetType) {
            Throwable current = exception;
            while (current != null) {
                if (targetType.isInstance(current)) {
                    return (T) current;
                }
                current = current.getCause();
            }
            return null;
        }
    }
}