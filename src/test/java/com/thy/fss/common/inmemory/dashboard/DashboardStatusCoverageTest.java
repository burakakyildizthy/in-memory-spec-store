package com.thy.fss.common.inmemory.dashboard;

import com.thy.fss.common.inmemory.dashboard.result.HealthStatus;
import com.thy.fss.common.inmemory.dashboard.status.DashboardStatus;
import com.thy.fss.common.inmemory.dashboard.status.SyncStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Coverage tests for DashboardStatus record targeting missed branches.
 */
class DashboardStatusCoverageTest {

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final String DASH_ID = "dash-1";
    private static final String DASH_NAME = "Dashboard";

    private DashboardStatus createBasic() {
        return new DashboardStatus(DASH_ID, DASH_NAME, null, null,
                SyncStatus.ACTIVE, HealthStatus.HEALTHY, null);
    }

    // ========== Constructor validation ==========

    @Test
    void constructor_withNullId_throwsNullPointerException() {
        assertThatThrownBy(() -> new DashboardStatus(null, DASH_NAME, null, null,
                SyncStatus.ACTIVE, HealthStatus.HEALTHY, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_withNullName_throwsNullPointerException() {
        assertThatThrownBy(() -> new DashboardStatus(DASH_ID, null, null, null,
                SyncStatus.ACTIVE, HealthStatus.HEALTHY, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_withNullSyncStatus_throwsNullPointerException() {
        assertThatThrownBy(() -> new DashboardStatus(DASH_ID, DASH_NAME, null, null,
                null, HealthStatus.HEALTHY, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_withNullHealthStatus_throwsNullPointerException() {
        assertThatThrownBy(() -> new DashboardStatus(DASH_ID, DASH_NAME, null, null,
                SyncStatus.ACTIVE, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_withNullErrors_createsWithEmptyErrors() {
        DashboardStatus status = createBasic();
        assertThat(status.errors()).isEmpty();
        assertThat(status.hasErrors()).isFalse();
    }

    @Test
    void constructor_withErrors_createsWithErrors() {
        DashboardStatus status = new DashboardStatus(DASH_ID, DASH_NAME, null, null,
                SyncStatus.ACTIVE, HealthStatus.HEALTHY, Map.of("key1", "error1"));
        assertThat(status.hasErrors()).isTrue();
        assertThat(status.errors()).containsKey("key1");
    }

    // ========== isActive ==========

    @Test
    void isActive_withActiveSyncAndHealthy_returnsTrue() {
        DashboardStatus status = new DashboardStatus(DASH_ID, DASH_NAME, null, null,
                SyncStatus.ACTIVE, HealthStatus.HEALTHY, null);
        assertThat(status.isActive()).isTrue();
    }

    @Test
    void isActive_withPausedSync_returnsFalse() {
        DashboardStatus status = new DashboardStatus(DASH_ID, DASH_NAME, null, null,
                SyncStatus.PAUSED, HealthStatus.HEALTHY, null);
        assertThat(status.isActive()).isFalse();
    }

    @Test
    void isActive_withActiveSyncButUnhealthy_returnsFalse() {
        DashboardStatus status = new DashboardStatus(DASH_ID, DASH_NAME, null, null,
                SyncStatus.ACTIVE, HealthStatus.ERROR, null);
        assertThat(status.isActive()).isFalse();
    }

    // ========== isSyncPaused ==========

    @Test
    void isSyncPaused_withPausedSync_returnsTrue() {
        DashboardStatus status = new DashboardStatus(DASH_ID, DASH_NAME, null, null,
                SyncStatus.PAUSED, HealthStatus.HEALTHY, null);
        assertThat(status.isSyncPaused()).isTrue();
    }

    @Test
    void isSyncPaused_withActiveSync_returnsFalse() {
        assertThat(createBasic().isSyncPaused()).isFalse();
    }

    // ========== hasBeenCalculated ==========

    @Test
    void hasBeenCalculated_withNullCalculationTime_returnsFalse() {
        assertThat(createBasic().hasBeenCalculated()).isFalse();
    }

    @Test
    void hasBeenCalculated_withNonNullCalculationTime_returnsTrue() {
        DashboardStatus status = new DashboardStatus(DASH_ID, DASH_NAME, NOW, null,
                SyncStatus.ACTIVE, HealthStatus.HEALTHY, null);
        assertThat(status.hasBeenCalculated()).isTrue();
    }

    // ========== hasBeenRead ==========

    @Test
    void hasBeenRead_withNullReadTime_returnsFalse() {
        assertThat(createBasic().hasBeenRead()).isFalse();
    }

    @Test
    void hasBeenRead_withNonNullReadTime_returnsTrue() {
        DashboardStatus status = new DashboardStatus(DASH_ID, DASH_NAME, null, NOW,
                SyncStatus.ACTIVE, HealthStatus.HEALTHY, null);
        assertThat(status.hasBeenRead()).isTrue();
    }

    // ========== getError and hasError ==========

    @Test
    void getError_withExistingKey_returnsMessage() {
        DashboardStatus status = new DashboardStatus(DASH_ID, DASH_NAME, null, null,
                SyncStatus.ACTIVE, HealthStatus.HEALTHY, Map.of("timeout", "Connection timed out"));
        assertThat(status.getError("timeout")).isEqualTo("Connection timed out");
    }

    @Test
    void getError_withNonExistentKey_returnsNull() {
        assertThat(createBasic().getError("nonexistent")).isNull();
    }

    @Test
    void hasError_withExistingKey_returnsTrue() {
        DashboardStatus status = new DashboardStatus(DASH_ID, DASH_NAME, null, null,
                SyncStatus.ACTIVE, HealthStatus.HEALTHY, Map.of("err", "msg"));
        assertThat(status.hasError("err")).isTrue();
    }

    @Test
    void hasError_withNonExistentKey_returnsFalse() {
        assertThat(createBasic().hasError("nonexistent")).isFalse();
    }

    // ========== equals ==========

    @Test
    void equals_withSameObject_returnsTrue() {
        DashboardStatus status = createBasic();
        assertThat(status.equals(status)).isTrue();
    }

    @Test
    void equals_withNull_returnsFalse() {
        assertThat(createBasic().equals(null)).isFalse();
    }

    @Test
    void equals_withDifferentClass_returnsFalse() {
        assertThat(createBasic().equals("not-a-status")).isFalse();
    }

    @Test
    void equals_withSameValues_returnsTrue() {
        DashboardStatus s1 = createBasic();
        DashboardStatus s2 = new DashboardStatus(DASH_ID, DASH_NAME, null, null,
                SyncStatus.ACTIVE, HealthStatus.HEALTHY, null);
        assertThat(s1.equals(s2)).isTrue();
    }

    @Test
    void equals_withDifferentId_returnsFalse() {
        DashboardStatus s1 = createBasic();
        DashboardStatus s2 = new DashboardStatus("other-id", DASH_NAME, null, null,
                SyncStatus.ACTIVE, HealthStatus.HEALTHY, null);
        assertThat(s1.equals(s2)).isFalse();
    }

    @Test
    void equals_withDifferentSyncStatus_returnsFalse() {
        DashboardStatus s1 = createBasic();
        DashboardStatus s2 = new DashboardStatus(DASH_ID, DASH_NAME, null, null,
                SyncStatus.PAUSED, HealthStatus.HEALTHY, null);
        assertThat(s1.equals(s2)).isFalse();
    }

    @Test
    void equals_withDifferentHealthStatus_returnsFalse() {
        DashboardStatus s1 = createBasic();
        DashboardStatus s2 = new DashboardStatus(DASH_ID, DASH_NAME, null, null,
                SyncStatus.ACTIVE, HealthStatus.ERROR, null);
        assertThat(s1.equals(s2)).isFalse();
    }

    // ========== toString ==========

    @Test
    void toString_containsId() {
        assertThat(createBasic().toString()).contains(DASH_ID);
    }
}
