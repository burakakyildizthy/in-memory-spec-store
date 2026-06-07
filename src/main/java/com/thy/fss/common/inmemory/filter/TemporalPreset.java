package com.thy.fss.common.inmemory.filter;

import java.util.Objects;

/**
 * Typed value object for temporal preset expressions such as 24h, 40m, 2M.
 */
public final class TemporalPreset {

    private final long amount;
    private final TemporalPresetUnit unit;

    public TemporalPreset(long amount, TemporalPresetUnit unit) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Temporal preset amount must be greater than 0");
        }
        this.amount = amount;
        this.unit = Objects.requireNonNull(unit, "Temporal preset unit cannot be null");
    }

    public long getAmount() {
        return amount;
    }

    public TemporalPresetUnit getUnit() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TemporalPreset that)) {
            return false;
        }
        return amount == that.amount && unit == that.unit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, unit);
    }

    @Override
    public String toString() {
        return amount + String.valueOf(unit.getSymbol());
    }
}