package com.thy.fss.common.inmemory.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AggregationType enum.
 */
@DisplayName("AggregationType Tests")
class AggregationTypeTest {

    private static final String COUNT = "COUNT";
    private static final String CUSTOM = "CUSTOM";
    private static final String SUM = "SUM";
    private static final String AVG = "AVG";
    private static final String MIN = "MIN";
    private static final String MAX = "MAX";
    @Test
    @DisplayName("Should have all expected aggregation types")
    void shouldHaveAllExpectedAggregationTypes() {
        AggregationType[] types = AggregationType.values();

        assertThat(types).hasSize(6)
                .containsExactlyInAnyOrder(
                        AggregationType.COUNT,
                        AggregationType.SUM,
                        AggregationType.AVG,
                        AggregationType.MIN,
                        AggregationType.MAX,
                        AggregationType.CUSTOM
                );
    }

    @Test
    @DisplayName("Should have correct enum names")
    void shouldHaveCorrectEnumNames() {
        assertThat(AggregationType.COUNT.name()).isEqualTo(COUNT);
        assertThat(AggregationType.SUM.name()).isEqualTo(SUM);
        assertThat(AggregationType.AVG.name()).isEqualTo(AVG);
        assertThat(AggregationType.MIN.name()).isEqualTo(MIN);
        assertThat(AggregationType.MAX.name()).isEqualTo(MAX);
        assertThat(AggregationType.CUSTOM.name()).isEqualTo(CUSTOM);
    }

    @Test
    @DisplayName("Should support valueOf operations")
    void shouldSupportValueOfOperations() {
        assertThat(AggregationType.valueOf(COUNT)).isEqualTo(AggregationType.COUNT);
        assertThat(AggregationType.valueOf(SUM)).isEqualTo(AggregationType.SUM);
        assertThat(AggregationType.valueOf(AVG)).isEqualTo(AggregationType.AVG);
        assertThat(AggregationType.valueOf(MIN)).isEqualTo(AggregationType.MIN);
        assertThat(AggregationType.valueOf(MAX)).isEqualTo(AggregationType.MAX);
        assertThat(AggregationType.valueOf(CUSTOM)).isEqualTo(AggregationType.CUSTOM);
    }

    @Test
    @DisplayName("Should have correct ordinal values")
    void shouldHaveCorrectOrdinalValues() {
        assertThat(AggregationType.COUNT.ordinal()).isZero();
        assertThat(AggregationType.SUM.ordinal()).isEqualTo(1);
        assertThat(AggregationType.AVG.ordinal()).isEqualTo(2);
        assertThat(AggregationType.MIN.ordinal()).isEqualTo(3);
        assertThat(AggregationType.MAX.ordinal()).isEqualTo(4);
        assertThat(AggregationType.CUSTOM.ordinal()).isEqualTo(5);
    }
}