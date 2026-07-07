package com.thy.fss.common.inmemory.dashboard.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SyncStatus enum.
 * Migrated from old_tests_backup/status/SyncStatusTest.java to new API structure.
 */
@DisplayName("SyncStatus Tests")
class SyncStatusTest {

    private static final String ACTIVE = "ACTIVE";
    private static final String PAUSED = "PAUSED";
    private static final String ERROR = "ERROR";
    

    @Test
    @DisplayName("Should have all expected sync statuses")
    void shouldHaveAllExpectedSyncStatuses() {
        SyncStatus[] statuses = SyncStatus.values();

        assertThat(statuses).hasSize(3)
                .containsExactlyInAnyOrder(
                        SyncStatus.ACTIVE,
                        SyncStatus.PAUSED,
                        SyncStatus.ERROR
                );
    }

    @Test
    @DisplayName("Should have correct enum names")
    void shouldHaveCorrectEnumNames() {
        assertThat(SyncStatus.ACTIVE.name()).isEqualTo(ACTIVE);
        assertThat(SyncStatus.PAUSED.name()).isEqualTo(PAUSED);
        assertThat(SyncStatus.ERROR.name()).isEqualTo(ERROR);
    }

    @Test
    @DisplayName("Should support valueOf operations")
    void shouldSupportValueOfOperations() {
        assertThat(SyncStatus.valueOf(ACTIVE)).isEqualTo(SyncStatus.ACTIVE);
        assertThat(SyncStatus.valueOf(PAUSED)).isEqualTo(SyncStatus.PAUSED);
        assertThat(SyncStatus.valueOf(ERROR)).isEqualTo(SyncStatus.ERROR);
    }

    @Test
    @DisplayName("Should have correct ordinal values")
    void shouldHaveCorrectOrdinalValues() {
        assertThat(SyncStatus.ACTIVE.ordinal()).isZero();
        assertThat(SyncStatus.PAUSED.ordinal()).isEqualTo(1);
        assertThat(SyncStatus.ERROR.ordinal()).isEqualTo(2);
    }
}