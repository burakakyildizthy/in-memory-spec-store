package com.thy.fss.common.inmemory.specification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Operator enum to verify all expected values exist.
 */
@DisplayName("Operator Enum Tests")
class OperatorTest {

    @Test
    @DisplayName("Should have negated numeric comparison operators defined")
    void shouldHaveNegatedNumericComparisonOperators() {
        assertThat(Operator.NOT_GREATER_THAN).isNotNull();
        assertThat(Operator.NOT_LESS_THAN).isNotNull();
        assertThat(Operator.NOT_GREATER_OR_EQUAL_THAN).isNotNull();
        assertThat(Operator.NOT_LESS_OR_EQUAL_THAN).isNotNull();
    }

    @Test
    @DisplayName("Should have negated temporal operators defined")
    void shouldHaveNegatedTemporalOperators() {
        assertThat(Operator.NOT_IS_BEFORE).isNotNull();
        assertThat(Operator.NOT_IS_AFTER).isNotNull();
        assertThat(Operator.NOT_IS_ON_OR_BEFORE).isNotNull();
        assertThat(Operator.NOT_IS_ON_OR_AFTER).isNotNull();
    }

    @Test
    @DisplayName("Should resolve negated numeric operators via valueOf")
    void shouldResolveNegatedNumericOperatorsViaValueOf() {
        assertThat(Operator.valueOf("NOT_GREATER_THAN")).isEqualTo(Operator.NOT_GREATER_THAN);
        assertThat(Operator.valueOf("NOT_LESS_THAN")).isEqualTo(Operator.NOT_LESS_THAN);
        assertThat(Operator.valueOf("NOT_GREATER_OR_EQUAL_THAN")).isEqualTo(Operator.NOT_GREATER_OR_EQUAL_THAN);
        assertThat(Operator.valueOf("NOT_LESS_OR_EQUAL_THAN")).isEqualTo(Operator.NOT_LESS_OR_EQUAL_THAN);
    }

    @Test
    @DisplayName("Should resolve negated temporal operators via valueOf")
    void shouldResolveNegatedTemporalOperatorsViaValueOf() {
        assertThat(Operator.valueOf("NOT_IS_BEFORE")).isEqualTo(Operator.NOT_IS_BEFORE);
        assertThat(Operator.valueOf("NOT_IS_AFTER")).isEqualTo(Operator.NOT_IS_AFTER);
        assertThat(Operator.valueOf("NOT_IS_ON_OR_BEFORE")).isEqualTo(Operator.NOT_IS_ON_OR_BEFORE);
        assertThat(Operator.valueOf("NOT_IS_ON_OR_AFTER")).isEqualTo(Operator.NOT_IS_ON_OR_AFTER);
    }
}
