package com.thy.fss.common.inmemory.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for temporal preset expressions such as 24h, 40m, 2M.
 */
public final class TemporalPresetParser {

    private static final Pattern PRESET_PATTERN = Pattern.compile("^([1-9]\\d*)([smhdwMy])$");

    private TemporalPresetParser() {
    }

    public static TemporalPreset parse(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Temporal preset expression cannot be null or empty");
        }

        String normalized = expression.trim();
        Matcher matcher = PRESET_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid temporal preset expression: " + expression +
                            ". Expected format like 24h, 40m, 7d, 2w, 3M, 1y"
            );
        }

        long amount = Long.parseLong(matcher.group(1));
        char unitSymbol = matcher.group(2).charAt(0);
        return new TemporalPreset(amount, TemporalPresetUnit.fromSymbol(unitSymbol));
    }
}