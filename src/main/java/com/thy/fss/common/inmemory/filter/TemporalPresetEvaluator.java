package com.thy.fss.common.inmemory.filter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Evaluates typed temporal preset windows for supported temporal field values.
 */
public final class TemporalPresetEvaluator {

    private TemporalPresetEvaluator() {
    }

    public static boolean matchesLast(Object fieldValue, TemporalPreset preset) {
        if (fieldValue == null || preset == null) {
            return false;
        }
        if (fieldValue instanceof LocalDate value) {
            LocalDate now = LocalDate.now();
            LocalDate from = shiftDate(now, preset, true);
            return !value.isBefore(from) && !value.isAfter(now);
        }
        if (fieldValue instanceof LocalDateTime value) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime from = shiftDateTime(now, preset, true);
            return !value.isBefore(from) && !value.isAfter(now);
        }
        if (fieldValue instanceof Instant value) {
            Instant now = Instant.now();
            Instant from = shiftInstant(now, preset, true);
            return !value.isBefore(from) && !value.isAfter(now);
        }
        throw new IllegalArgumentException("Unsupported temporal field type for LAST: " + fieldValue.getClass().getName());
    }

    public static boolean matchesNext(Object fieldValue, TemporalPreset preset) {
        if (fieldValue == null || preset == null) {
            return false;
        }
        if (fieldValue instanceof LocalDate value) {
            LocalDate now = LocalDate.now();
            LocalDate to = shiftDate(now, preset, false);
            return !value.isBefore(now) && !value.isAfter(to);
        }
        if (fieldValue instanceof LocalDateTime value) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime to = shiftDateTime(now, preset, false);
            return !value.isBefore(now) && !value.isAfter(to);
        }
        if (fieldValue instanceof Instant value) {
            Instant now = Instant.now();
            Instant to = shiftInstant(now, preset, false);
            return !value.isBefore(now) && !value.isAfter(to);
        }
        throw new IllegalArgumentException("Unsupported temporal field type for NEXT: " + fieldValue.getClass().getName());
    }

    private static LocalDate shiftDate(LocalDate now, TemporalPreset preset, boolean backward) {
        LocalDateTime nowDateTime = LocalDateTime.now();
        return shiftDateTime(nowDateTime, preset, backward).toLocalDate();
    }

    private static LocalDateTime shiftDateTime(LocalDateTime now, TemporalPreset preset, boolean backward) {
        long amount = preset.getAmount();
        return switch (preset.getUnit()) {
            case SECOND -> backward ? now.minusSeconds(amount) : now.plusSeconds(amount);
            case MINUTE -> backward ? now.minusMinutes(amount) : now.plusMinutes(amount);
            case HOUR -> backward ? now.minusHours(amount) : now.plusHours(amount);
            case DAY -> backward ? now.minusDays(amount) : now.plusDays(amount);
            case WEEK -> backward ? now.minusWeeks(amount) : now.plusWeeks(amount);
            case MONTH -> backward ? now.minusMonths(amount) : now.plusMonths(amount);
            case YEAR -> backward ? now.minusYears(amount) : now.plusYears(amount);
        };
    }

    private static Instant shiftInstant(Instant now, TemporalPreset preset, boolean backward) {
        long amount = preset.getAmount();
        return switch (preset.getUnit()) {
            case SECOND -> backward ? now.minusSeconds(amount) : now.plusSeconds(amount);
            case MINUTE -> backward ? now.minusSeconds(amount * 60) : now.plusSeconds(amount * 60);
            case HOUR -> backward ? now.minusSeconds(amount * 3600) : now.plusSeconds(amount * 3600);
            case DAY -> backward ? now.minusSeconds(amount * 86400) : now.plusSeconds(amount * 86400);
            case WEEK -> backward ? now.minusSeconds(amount * 604800) : now.plusSeconds(amount * 604800);
            case MONTH -> shiftInstantWithCalendar(now, amount, backward, true);
            case YEAR -> shiftInstantWithCalendar(now, amount, backward, false);
        };
    }

    private static Instant shiftInstantWithCalendar(Instant now, long amount, boolean backward, boolean monthBased) {
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(now, ZoneId.systemDefault());
        ZonedDateTime shifted = monthBased
                ? (backward ? zonedDateTime.minusMonths(amount) : zonedDateTime.plusMonths(amount))
                : (backward ? zonedDateTime.minusYears(amount) : zonedDateTime.plusYears(amount));
        return shifted.toInstant();
    }
}