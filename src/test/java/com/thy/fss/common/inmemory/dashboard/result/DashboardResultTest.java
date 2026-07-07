package com.thy.fss.common.inmemory.dashboard.result;

import com.thy.fss.common.inmemory.common.generator.TestDataGenerator;
import com.thy.fss.common.inmemory.common.model.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for DashboardResult class to improve coverage.
 * Migrated from old_tests_backup/result/DashboardResultTest.java to new API structure.
 * Updated to use common test infrastructure and TestUser model.
 */
@DisplayName("DashboardResult Tests")
class DashboardResultTest {
    
    private static final String ERROR = "error";
    private static final String FIELD1 = "field1";
    private static final String FIELD2 = "field2";
    private static final String FIELD_1_ERROR = "Field 1 error";
    private static final String FIELD_2_ERROR = "Field 2 error";
    private static final String SOME_ERROR = "Some error";
    private static final String NONEXISTENT = "nonexistent";

    private TestUser testUser;

    @BeforeEach
    void setUp() {
        TestDataGenerator.resetCounters();
        testUser = TestDataGenerator.createUser("John Doe", 30);
    }

    @Test
    @DisplayName("Should create DashboardResult with all parameters")
    void testCreateDashboardResultWithAllParameters() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();
        Map<String, String> errors = new HashMap<>();
        errors.put("test_error", "Test error message");

        // When
        DashboardResult<TestUser> result = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.HEALTHY, 1L, errors
        );

        // Then
        assertThat(result.data()).isEqualTo(testUser);
        assertThat(result.lastCalculationTime()).isEqualTo(calculationTime);
        assertThat(result.healthStatus()).isEqualTo(HealthStatus.HEALTHY);
        assertThat(result.dataVersion()).isEqualTo(1L);
        assertThat(result.errors()).isEqualTo(errors);
    }

    @Test
    @DisplayName("Should handle null data")
    void testNullData() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();
        Map<String, String> errors = new HashMap<>();

        // When
        DashboardResult<TestUser> result = new DashboardResult<>(
                null, calculationTime, HealthStatus.ERROR, 1L, errors
        );

        // Then
        assertThat(result.data()).isNull();
        assertThat(result.healthStatus()).isEqualTo(HealthStatus.ERROR);
    }

    @Test
    @DisplayName("Should test hasErrors method")
    void testHasErrors() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();

        Map<String, String> emptyErrors = new HashMap<>();
        Map<String, String> withErrors = new HashMap<>();
        withErrors.put(ERROR, SOME_ERROR);

        // When
        DashboardResult<TestUser> resultWithoutErrors = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.HEALTHY, 1L, emptyErrors
        );

        DashboardResult<TestUser> resultWithErrors = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.ERROR, 1L, withErrors
        );

        // Then
        assertThat(resultWithoutErrors.hasErrors()).isFalse();
        assertThat(resultWithErrors.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("Should test isHealthy method")
    void testIsHealthy() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();
        Map<String, String> errors = new HashMap<>();

        // When
        DashboardResult<TestUser> healthyResult = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.HEALTHY, 1L, errors
        );

        DashboardResult<TestUser> errorResult = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.ERROR, 1L, errors
        );

        // Then
        assertThat(healthyResult.isHealthy()).isTrue();
        assertThat(errorResult.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should test getFieldError method")
    void testGetFieldError() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();
        Map<String, String> errors = new HashMap<>();
        errors.put(FIELD1, FIELD_1_ERROR);
        errors.put(FIELD2, FIELD_2_ERROR);

        DashboardResult<TestUser> result = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.ERROR, 1L, errors
        );

        // When & Then
        assertThat(result.getFieldError(FIELD1)).isEqualTo(FIELD_1_ERROR);
        assertThat(result.getFieldError(FIELD2)).isEqualTo(FIELD_2_ERROR);
        assertThat(result.getFieldError(NONEXISTENT)).isNull();
    }

    @Test
    @DisplayName("Should test hasFieldError method")
    void testHasFieldError() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();
        Map<String, String> errors = new HashMap<>();
        errors.put(FIELD1, FIELD_1_ERROR);

        DashboardResult<TestUser> result = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.ERROR, 1L, errors
        );

        // When & Then
        assertThat(result.hasFieldError(FIELD1)).isTrue();
        assertThat(result.hasFieldError(NONEXISTENT)).isFalse();
    }

    @Test
    @DisplayName("Should throw exception for null calculation time")
    void testNullCalculationTime() {
        // When & Then
        Map<String, String> errors = new HashMap<>();

        // When & Then
        assertThrows(NullPointerException.class, () ->
                new DashboardResult<>(testUser, null, HealthStatus.HEALTHY, 1L, errors)
        );
    }

    @Test
    @DisplayName("Should throw exception for null health status")
    void testNullHealthStatus() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();
        Map<String, String> errors = new HashMap<>();

        // When & Then
        assertThrows(NullPointerException.class, () ->
                new DashboardResult<>(testUser, calculationTime, null, 1L, errors)
        );
    }

    @Test
    @DisplayName("Should throw exception for negative version")
    void testNegativeVersion() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();
        Map<String, String> errors = new HashMap<>();

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                new DashboardResult<>(testUser, calculationTime, HealthStatus.HEALTHY, -1L, errors)
        );
    }

    @Test
    @DisplayName("Should handle null errors map")
    void testNullErrorsMap() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();

        // When - null errors map should be handled gracefully
        DashboardResult<TestUser> result = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.HEALTHY, 1L, null
        );

        // Then - should create empty errors map
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("Should test equals and hashCode")
    void testEqualsAndHashCode() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();
        Map<String, String> errors = new HashMap<>();

        DashboardResult<TestUser> result1 = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.HEALTHY, 1L, errors
        );

        DashboardResult<TestUser> result2 = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.HEALTHY, 1L, errors
        );

        DashboardResult<TestUser> result3 = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.ERROR, 1L, errors
        );

        // Then
        assertThat(result1).isEqualTo(result2)
                .hasSameHashCodeAs(result2)
                .isNotEqualTo(result3)
                .doesNotHaveSameHashCodeAs(result3);
    }

    @Test
    @DisplayName("Should test toString")
    void testToString() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();
        Map<String, String> errors = new HashMap<>();

        DashboardResult<TestUser> result = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.HEALTHY, 1L, errors
        );

        // When
        String toString = result.toString();

        // Then
        assertThat(toString).contains("DashboardResult")
                .contains("HEALTHY")
                .contains("dataVersion=1");
    }

    @Test
    @DisplayName("Should test version zero")
    void testVersionZero() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();

        // When
        DashboardResult<TestUser> result = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.HEALTHY, 0L, new HashMap<>()
        );

        // Then
        assertThat(result.dataVersion()).isZero();
    }

    @Test
    @DisplayName("Should handle empty errors map")
    void testEmptyErrorsMap() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();
        Map<String, String> emptyErrors = new HashMap<>();

        // When
        DashboardResult<TestUser> result = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.HEALTHY, 1L, emptyErrors
        );

        // Then
        assertThat(result.errors()).isEmpty();
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.hasFieldError("any")).isFalse();
        assertThat(result.getFieldError("any")).isNull();
    }

    @Test
    @DisplayName("Should test isHealthy with null data")
    void testIsHealthyWithNullData() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();
        Map<String, String> errors = new HashMap<>();

        // When
        DashboardResult<TestUser> result = new DashboardResult<>(
                null, calculationTime, HealthStatus.HEALTHY, 1L, errors
        );

        // Then - should not be healthy if data is null
        assertThat(result.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should test isHealthy with errors")
    void testIsHealthyWithErrors() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();
        Map<String, String> errors = new HashMap<>();
        errors.put(ERROR, SOME_ERROR);

        // When
        DashboardResult<TestUser> result = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.HEALTHY, 1L, errors
        );

        // Then - should not be healthy if there are errors
        assertThat(result.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should test calculating status")
    void testCalculatingStatus() {
        // Given
        LocalDateTime calculationTime = LocalDateTime.now();
        Map<String, String> errors = new HashMap<>();

        // When
        DashboardResult<TestUser> result = new DashboardResult<>(
                testUser, calculationTime, HealthStatus.CALCULATING, 1L, errors
        );

        // Then
        assertThat(result.healthStatus()).isEqualTo(HealthStatus.CALCULATING);
        assertThat(result.isHealthy()).isFalse();
    }
}