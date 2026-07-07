package com.thy.fss.common.inmemory.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TemporalPresetEvaluator Tests")
class TemporalPresetEvaluatorTest {

    // ===== matchesLast =====

    @Test
    void matchesLast_nullFieldValue_returnsFalse() {
        assertThat(TemporalPresetEvaluator.matchesLast(null, new TemporalPreset(1, TemporalPresetUnit.DAY))).isFalse();
    }

    @Test
    void matchesLast_nullPreset_returnsFalse() {
        assertThat(TemporalPresetEvaluator.matchesLast(LocalDate.now(), null)).isFalse();
    }

    @Test
    void matchesLast_localDate_withinWindow_returnsTrue() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        assertThat(TemporalPresetEvaluator.matchesLast(yesterday, new TemporalPreset(2, TemporalPresetUnit.DAY))).isTrue();
    }

    @Test
    void matchesLast_localDate_outsideWindow_returnsFalse() {
        LocalDate farPast = LocalDate.now().minusDays(30);
        assertThat(TemporalPresetEvaluator.matchesLast(farPast, new TemporalPreset(7, TemporalPresetUnit.DAY))).isFalse();
    }

    @Test
    void matchesLast_localDateTime_withinWindow_returnsTrue() {
        LocalDateTime recent = LocalDateTime.now().minusHours(1);
        assertThat(TemporalPresetEvaluator.matchesLast(recent, new TemporalPreset(2, TemporalPresetUnit.HOUR))).isTrue();
    }

    @Test
    void matchesLast_localDateTime_outsideWindow_returnsFalse() {
        LocalDateTime old = LocalDateTime.now().minusDays(10);
        assertThat(TemporalPresetEvaluator.matchesLast(old, new TemporalPreset(1, TemporalPresetUnit.DAY))).isFalse();
    }

    @Test
    void matchesLast_instant_withinWindow_returnsTrue() {
        Instant recent = Instant.now().minusSeconds(60);
        assertThat(TemporalPresetEvaluator.matchesLast(recent, new TemporalPreset(5, TemporalPresetUnit.MINUTE))).isTrue();
    }

    @Test
    void matchesLast_instant_outsideWindow_returnsFalse() {
        Instant old = Instant.now().minusSeconds(3600L * 48);
        assertThat(TemporalPresetEvaluator.matchesLast(old, new TemporalPreset(1, TemporalPresetUnit.HOUR))).isFalse();
    }

    @Test
    void matchesLast_localDateTime_second_withinWindow() {
        LocalDateTime recent = LocalDateTime.now().minusSeconds(5);
        assertThat(TemporalPresetEvaluator.matchesLast(recent, new TemporalPreset(10, TemporalPresetUnit.SECOND))).isTrue();
    }

    @Test
    void matchesLast_localDateTime_minute_withinWindow() {
        LocalDateTime recent = LocalDateTime.now().minusMinutes(2);
        assertThat(TemporalPresetEvaluator.matchesLast(recent, new TemporalPreset(5, TemporalPresetUnit.MINUTE))).isTrue();
    }

    @Test
    void matchesLast_localDateTime_week_withinWindow() {
        LocalDateTime recent = LocalDateTime.now().minusDays(3);
        assertThat(TemporalPresetEvaluator.matchesLast(recent, new TemporalPreset(1, TemporalPresetUnit.WEEK))).isTrue();
    }

    @Test
    void matchesLast_localDateTime_month_withinWindow() {
        LocalDateTime recent = LocalDateTime.now().minusDays(10);
        assertThat(TemporalPresetEvaluator.matchesLast(recent, new TemporalPreset(1, TemporalPresetUnit.MONTH))).isTrue();
    }

    @Test
    void matchesLast_localDateTime_year_withinWindow() {
        LocalDateTime recent = LocalDateTime.now().minusMonths(3);
        assertThat(TemporalPresetEvaluator.matchesLast(recent, new TemporalPreset(1, TemporalPresetUnit.YEAR))).isTrue();
    }

    @Test
    void matchesLast_instant_second_withinWindow() {
        Instant recent = Instant.now().minusSeconds(5);
        assertThat(TemporalPresetEvaluator.matchesLast(recent, new TemporalPreset(10, TemporalPresetUnit.SECOND))).isTrue();
    }

    @Test
    void matchesLast_instant_minute_withinWindow() {
        Instant recent = Instant.now().minusSeconds(60);
        assertThat(TemporalPresetEvaluator.matchesLast(recent, new TemporalPreset(2, TemporalPresetUnit.MINUTE))).isTrue();
    }

    @Test
    void matchesLast_instant_hour_withinWindow() {
        Instant recent = Instant.now().minusSeconds(1800);
        assertThat(TemporalPresetEvaluator.matchesLast(recent, new TemporalPreset(1, TemporalPresetUnit.HOUR))).isTrue();
    }

    @Test
    void matchesLast_instant_day_withinWindow() {
        Instant recent = Instant.now().minusSeconds(3600L * 12);
        assertThat(TemporalPresetEvaluator.matchesLast(recent, new TemporalPreset(1, TemporalPresetUnit.DAY))).isTrue();
    }

    @Test
    void matchesLast_instant_week_withinWindow() {
        Instant recent = Instant.now().minusSeconds(3600L * 24 * 3);
        assertThat(TemporalPresetEvaluator.matchesLast(recent, new TemporalPreset(1, TemporalPresetUnit.WEEK))).isTrue();
    }

    @Test
    void matchesLast_instant_month_withinWindow() {
        Instant recent = Instant.now().minusSeconds(3600L * 24 * 10);
        assertThat(TemporalPresetEvaluator.matchesLast(recent, new TemporalPreset(1, TemporalPresetUnit.MONTH))).isTrue();
    }

    @Test
    void matchesLast_instant_year_withinWindow() {
        Instant recent = Instant.now().minusSeconds(3600L * 24 * 100);
        assertThat(TemporalPresetEvaluator.matchesLast(recent, new TemporalPreset(1, TemporalPresetUnit.YEAR))).isTrue();
    }

    @Test
    void matchesLast_unsupportedType_throwsException() {
        assertThatThrownBy(() -> TemporalPresetEvaluator.matchesLast("not-temporal", new TemporalPreset(1, TemporalPresetUnit.DAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported temporal field type for LAST");
    }

    // ===== matchesNext =====

    @Test
    void matchesNext_nullFieldValue_returnsFalse() {
        assertThat(TemporalPresetEvaluator.matchesNext(null, new TemporalPreset(1, TemporalPresetUnit.DAY))).isFalse();
    }

    @Test
    void matchesNext_nullPreset_returnsFalse() {
        assertThat(TemporalPresetEvaluator.matchesNext(LocalDate.now(), null)).isFalse();
    }

    @Test
    void matchesNext_localDate_withinWindow_returnsTrue() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertThat(TemporalPresetEvaluator.matchesNext(tomorrow, new TemporalPreset(2, TemporalPresetUnit.DAY))).isTrue();
    }

    @Test
    void matchesNext_localDate_outsideWindow_returnsFalse() {
        LocalDate farFuture = LocalDate.now().plusDays(30);
        assertThat(TemporalPresetEvaluator.matchesNext(farFuture, new TemporalPreset(7, TemporalPresetUnit.DAY))).isFalse();
    }

    @Test
    void matchesNext_localDateTime_withinWindow_returnsTrue() {
        LocalDateTime soon = LocalDateTime.now().plusHours(1);
        assertThat(TemporalPresetEvaluator.matchesNext(soon, new TemporalPreset(2, TemporalPresetUnit.HOUR))).isTrue();
    }

    @Test
    void matchesNext_localDateTime_outsideWindow_returnsFalse() {
        LocalDateTime farFuture = LocalDateTime.now().plusDays(10);
        assertThat(TemporalPresetEvaluator.matchesNext(farFuture, new TemporalPreset(1, TemporalPresetUnit.DAY))).isFalse();
    }

    @Test
    void matchesNext_instant_withinWindow_returnsTrue() {
        Instant soon = Instant.now().plusSeconds(60);
        assertThat(TemporalPresetEvaluator.matchesNext(soon, new TemporalPreset(5, TemporalPresetUnit.MINUTE))).isTrue();
    }

    @Test
    void matchesNext_instant_outsideWindow_returnsFalse() {
        Instant farFuture = Instant.now().plusSeconds(3600L * 48);
        assertThat(TemporalPresetEvaluator.matchesNext(farFuture, new TemporalPreset(1, TemporalPresetUnit.HOUR))).isFalse();
    }

    @Test
    void matchesNext_localDateTime_second_withinWindow() {
        LocalDateTime soon = LocalDateTime.now().plusSeconds(5);
        assertThat(TemporalPresetEvaluator.matchesNext(soon, new TemporalPreset(10, TemporalPresetUnit.SECOND))).isTrue();
    }

    @Test
    void matchesNext_localDateTime_minute_withinWindow() {
        LocalDateTime soon = LocalDateTime.now().plusMinutes(2);
        assertThat(TemporalPresetEvaluator.matchesNext(soon, new TemporalPreset(5, TemporalPresetUnit.MINUTE))).isTrue();
    }

    @Test
    void matchesNext_localDateTime_week_withinWindow() {
        LocalDateTime soon = LocalDateTime.now().plusDays(3);
        assertThat(TemporalPresetEvaluator.matchesNext(soon, new TemporalPreset(1, TemporalPresetUnit.WEEK))).isTrue();
    }

    @Test
    void matchesNext_localDateTime_month_withinWindow() {
        LocalDateTime soon = LocalDateTime.now().plusDays(10);
        assertThat(TemporalPresetEvaluator.matchesNext(soon, new TemporalPreset(1, TemporalPresetUnit.MONTH))).isTrue();
    }

    @Test
    void matchesNext_localDateTime_year_withinWindow() {
        LocalDateTime soon = LocalDateTime.now().plusMonths(3);
        assertThat(TemporalPresetEvaluator.matchesNext(soon, new TemporalPreset(1, TemporalPresetUnit.YEAR))).isTrue();
    }

    @Test
    void matchesNext_instant_second_withinWindow() {
        Instant soon = Instant.now().plusSeconds(5);
        assertThat(TemporalPresetEvaluator.matchesNext(soon, new TemporalPreset(10, TemporalPresetUnit.SECOND))).isTrue();
    }

    @Test
    void matchesNext_instant_minute_withinWindow() {
        Instant soon = Instant.now().plusSeconds(60);
        assertThat(TemporalPresetEvaluator.matchesNext(soon, new TemporalPreset(2, TemporalPresetUnit.MINUTE))).isTrue();
    }

    @Test
    void matchesNext_instant_hour_withinWindow() {
        Instant soon = Instant.now().plusSeconds(1800);
        assertThat(TemporalPresetEvaluator.matchesNext(soon, new TemporalPreset(1, TemporalPresetUnit.HOUR))).isTrue();
    }

    @Test
    void matchesNext_instant_day_withinWindow() {
        Instant soon = Instant.now().plusSeconds(3600L * 12);
        assertThat(TemporalPresetEvaluator.matchesNext(soon, new TemporalPreset(1, TemporalPresetUnit.DAY))).isTrue();
    }

    @Test
    void matchesNext_instant_week_withinWindow() {
        Instant soon = Instant.now().plusSeconds(3600L * 24 * 3);
        assertThat(TemporalPresetEvaluator.matchesNext(soon, new TemporalPreset(1, TemporalPresetUnit.WEEK))).isTrue();
    }

    @Test
    void matchesNext_instant_month_withinWindow() {
        Instant soon = Instant.now().plusSeconds(3600L * 24 * 10);
        assertThat(TemporalPresetEvaluator.matchesNext(soon, new TemporalPreset(1, TemporalPresetUnit.MONTH))).isTrue();
    }

    @Test
    void matchesNext_instant_year_withinWindow() {
        Instant soon = Instant.now().plusSeconds(3600L * 24 * 100);
        assertThat(TemporalPresetEvaluator.matchesNext(soon, new TemporalPreset(1, TemporalPresetUnit.YEAR))).isTrue();
    }

    @Test
    void matchesNext_unsupportedType_throwsException() {
        assertThatThrownBy(() -> TemporalPresetEvaluator.matchesNext("not-temporal", new TemporalPreset(1, TemporalPresetUnit.DAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported temporal field type for NEXT");
    }
}
