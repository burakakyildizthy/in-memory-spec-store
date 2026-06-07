package com.thy.fss.common.inmemory.dashboard.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HealthStatus enum.
 * Migrated from old_tests_backup/result/HealthStatusTest.java to new API structure.
 */
@DisplayName("HealthStatus Tests")
class HealthStatusTest {

    private static final String HEALTHY = "HEALTHY";
    private static final String ERROR = "ERROR";
    private static final String CALCULATING = "CALCULATING";
    

    @Test
    @DisplayName("Should have all expected health statuses")
    void shouldHaveAllExpectedHealthStatuses() {
        HealthStatus[] statuses = HealthStatus.values();

        assertThat(statuses).hasSize(3).containsExactlyInAnyOrder(
                HealthStatus.HEALTHY,
                HealthStatus.ERROR,
                HealthStatus.CALCULATING
        );
    }

    @Test
    @DisplayName("Should have correct enum names")
    void shouldHaveCorrectEnumNames() {
        assertThat(HealthStatus.HEALTHY.name()).isEqualTo(HEALTHY);
        assertThat(HealthStatus.ERROR.name()).isEqualTo(ERROR);
        assertThat(HealthStatus.CALCULATING.name()).isEqualTo(CALCULATING);
    }

    @Test
    @DisplayName("Should support valueOf operations")
    void shouldSupportValueOfOperations() {
        assertThat(HealthStatus.valueOf(HEALTHY)).isEqualTo(HealthStatus.HEALTHY);
        assertThat(HealthStatus.valueOf(ERROR)).isEqualTo(HealthStatus.ERROR);
        assertThat(HealthStatus.valueOf(CALCULATING)).isEqualTo(HealthStatus.CALCULATING);
    }

    @Test
    @DisplayName("Should have correct ordinal values")
    void shouldHaveCorrectOrdinalValues() {
        assertThat(HealthStatus.HEALTHY.ordinal()).isZero();
        assertThat(HealthStatus.ERROR.ordinal()).isEqualTo(1);
        assertThat(HealthStatus.CALCULATING.ordinal()).isEqualTo(2);
    }
}