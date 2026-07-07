package com.thy.fss.common.inmemory.dashboard.status;

import com.thy.fss.common.inmemory.dashboard.result.HealthStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for DashboardStatus class.
 * Migrated from old_tests_backup/status/DashboardStatusTest.java to new API structure.
 */
@DisplayName("DashboardStatus Tests")
class DashboardStatusTest {

    private static final String DASHBOARD_1 = "dashboard1";
    private static final String TEST_DASHBOARD = "Test Dashboard";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String TEST = "test";
    private static final String CONNECTION = "connection";
    private static final String CONNECTION_FAILED = "Connection failed";
    private static final String NONEXISTNENT = "nonexistent";
    

    @Test
    @DisplayName("Should create dashboard status with all parameters")
    void shouldCreateDashboardStatusWithAllParameters() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> errors = new HashMap<>();
        errors.put("error1", "Test error");

        DashboardStatus status = new DashboardStatus(
                DASHBOARD_1,
                TEST_DASHBOARD,
                now,
                now.minusMinutes(5),
                SyncStatus.ACTIVE,
                HealthStatus.HEALTHY,
                errors
        );

        assertThat(status.id()).isEqualTo(DASHBOARD_1);
        assertThat(status.name()).isEqualTo(TEST_DASHBOARD);
        assertThat(status.lastCalculationTime()).isEqualTo(now);
        assertThat(status.lastReadTime()).isEqualTo(now.minusMinutes(5));
        assertThat(status.syncStatus()).isEqualTo(SyncStatus.ACTIVE);
        assertThat(status.healthStatus()).isEqualTo(HealthStatus.HEALTHY);
        assertThat(status.errors()).hasSize(1);
        assertThat(status.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("Should handle null timestamps")
    void shouldHandleNullTimestamps() {
        DashboardStatus status = new DashboardStatus(
                DASHBOARD_1,
                TEST_DASHBOARD,
                null,
                null,
                SyncStatus.PAUSED,
                HealthStatus.ERROR,
                null
        );

        assertThat(status.lastCalculationTime()).isNull();
        assertThat(status.lastReadTime()).isNull();
        assertThat(status.hasBeenCalculated()).isFalse();
        assertThat(status.hasBeenRead()).isFalse();
        assertThat(status.errors()).isEmpty();
        assertThat(status.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Should validate required parameters")
    void shouldValidateRequiredParameters() {
        assertThatThrownBy(() -> new DashboardStatus(
                null, TEST, null, null, SyncStatus.ACTIVE, HealthStatus.HEALTHY, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Dashboard ID cannot be null");

        assertThatThrownBy(() -> new DashboardStatus(
                ID, null, null, null, SyncStatus.ACTIVE, HealthStatus.HEALTHY, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Dashboard name cannot be null");

        assertThatThrownBy(() -> new DashboardStatus(
                ID, NAME, null, null, null, HealthStatus.HEALTHY, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Sync status cannot be null");

        assertThatThrownBy(() -> new DashboardStatus(
                ID, NAME, null, null, SyncStatus.ACTIVE, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Health status cannot be null");
    }

    @Test
    @DisplayName("Should check if dashboard is active")
    void shouldCheckIfDashboardIsActive() {
        DashboardStatus activeStatus = new DashboardStatus(
                ID, NAME, null, null, SyncStatus.ACTIVE, HealthStatus.HEALTHY, null);
        assertThat(activeStatus.isActive()).isTrue();

        DashboardStatus pausedStatus = new DashboardStatus(
                ID, NAME, null, null, SyncStatus.PAUSED, HealthStatus.HEALTHY, null);
        assertThat(pausedStatus.isActive()).isFalse();

        DashboardStatus errorStatus = new DashboardStatus(
                ID, NAME, null, null, SyncStatus.ACTIVE, HealthStatus.ERROR, null);
        assertThat(errorStatus.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should check sync pause status")
    void shouldCheckSyncPauseStatus() {
        DashboardStatus pausedStatus = new DashboardStatus(
                ID, NAME, null, null, SyncStatus.PAUSED, HealthStatus.HEALTHY, null);
        assertThat(pausedStatus.isSyncPaused()).isTrue();

        DashboardStatus activeStatus = new DashboardStatus(
                ID, NAME, null, null, SyncStatus.ACTIVE, HealthStatus.HEALTHY, null);
        assertThat(activeStatus.isSyncPaused()).isFalse();
    }

    @Test
    @DisplayName("Should handle error operations")
    void shouldHandleErrorOperations() {
        Map<String, String> errors = new HashMap<>();
        errors.put(CONNECTION, CONNECTION_FAILED);
        errors.put("calculation", "Calculation error");

        DashboardStatus status = new DashboardStatus(
                ID, NAME, null, null, SyncStatus.ERROR, HealthStatus.ERROR, errors);

        assertThat(status.hasError(CONNECTION)).isTrue();
        assertThat(status.hasError(NONEXISTNENT)).isFalse();
        assertThat(status.getError(CONNECTION)).isEqualTo(CONNECTION_FAILED);
        assertThat(status.getError(NONEXISTNENT)).isNull();
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> errors = new HashMap<>();
        errors.put(TEST, "error");

        DashboardStatus status1 = new DashboardStatus(
                ID, NAME, now, now, SyncStatus.ACTIVE, HealthStatus.HEALTHY, errors);
        DashboardStatus status2 = new DashboardStatus(
                ID, NAME, now, now, SyncStatus.ACTIVE, HealthStatus.HEALTHY, errors);
        DashboardStatus status3 = new DashboardStatus(
                "id2", NAME, now, now, SyncStatus.ACTIVE, HealthStatus.HEALTHY, errors);

        assertThat(status1).isEqualTo(status2)
                .isNotEqualTo(status3)
                .hasSameHashCodeAs(status2);
    }

    @Test
    @DisplayName("Should have meaningful toString")
    void shouldHaveMeaningfulToString() {
        DashboardStatus status = new DashboardStatus(
                ID, NAME, null, null, SyncStatus.ACTIVE, HealthStatus.HEALTHY, null);

        String toString = status.toString();
        assertThat(toString).contains("id='id'")
                .contains("name='name'")
                .contains("syncStatus=ACTIVE")
                .contains("healthStatus=HEALTHY");
    }
}