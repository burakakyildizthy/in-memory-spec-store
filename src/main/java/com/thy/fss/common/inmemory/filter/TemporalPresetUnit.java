package com.thy.fss.common.inmemory.filter;

/**
 * Supported preset units for temporal last/next expressions.
 */
public enum TemporalPresetUnit {
    SECOND('s'),
    MINUTE('m'),
    HOUR('h'),
    DAY('d'),
    WEEK('w'),
    MONTH('M'),
    YEAR('y');

    private final char symbol;

    TemporalPresetUnit(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }

    public static TemporalPresetUnit fromSymbol(char symbol) {
        for (TemporalPresetUnit unit : values()) {
            if (unit.symbol == symbol) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Unsupported temporal preset unit: " + symbol);
    }
}