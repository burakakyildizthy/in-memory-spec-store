package com.thy.fss.common.inmemory.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TemporalFilter Tests")
class TemporalFilterTest {

    @Test
    @DisplayName("Copy constructor should include last and next presets")
    void copyConstructorShouldIncludePresets() {
        TemporalFilter<LocalDateTime> source = new TemporalFilter<LocalDateTime>()
                .setIsBefore(LocalDateTime.of(2026, 1, 1, 0, 0))
                .setIsAfter(LocalDateTime.of(2025, 1, 1, 0, 0))
                .setLast(new TemporalPreset(24, TemporalPresetUnit.HOUR))
                .setNext(new TemporalPreset(40, TemporalPresetUnit.MINUTE));

        TemporalFilter<LocalDateTime> copy = new TemporalFilter<>(source);

        assertThat(copy).isEqualTo(source);
        assertThat(copy.getLast()).isEqualTo(new TemporalPreset(24, TemporalPresetUnit.HOUR));
        assertThat(copy.getNext()).isEqualTo(new TemporalPreset(40, TemporalPresetUnit.MINUTE));
    }
}
