package com.thy.fss.common.inmemory.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TemporalPresetParser Tests")
class TemporalPresetParserTest {

    @Test
    @DisplayName("Should parse valid temporal preset expression")
    void shouldParseValidPresetExpression() {
        TemporalPreset preset = TemporalPresetParser.parse("24h");

        assertThat(preset.getAmount()).isEqualTo(24L);
        assertThat(preset.getUnit()).isEqualTo(TemporalPresetUnit.HOUR);
    }

    @Test
    @DisplayName("Should parse month with uppercase M")
    void shouldParseMonthPreset() {
        TemporalPreset preset = TemporalPresetParser.parse("3M");

        assertThat(preset.getAmount()).isEqualTo(3L);
        assertThat(preset.getUnit()).isEqualTo(TemporalPresetUnit.MONTH);
    }

    @Test
    @DisplayName("Should reject invalid temporal preset expression")
    void shouldRejectInvalidPresetExpression() {
        assertThatThrownBy(() -> TemporalPresetParser.parse("0h"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> TemporalPresetParser.parse("10x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
