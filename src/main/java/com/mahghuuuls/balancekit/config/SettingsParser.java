package com.mahghuuuls.balancekit.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Turns the raw text of each setting into validated values. Owns the accepted ranges; an invalid value is reported
 * and replaced by its neutral value, never silently repaired to something else.
 */
public final class SettingsParser {

    public static final String MOBS_HEALTH = "mobs.health";

    private SettingsParser() {
    }

    public static Result parse(Map<String, String> raw) {
        List<ConfigWarning> warnings = new ArrayList<>();
        double mobHealth = positiveMultiplier(MOBS_HEALTH, raw.get(MOBS_HEALTH), warnings);
        return new Result(new BalanceSettings(mobHealth), warnings);
    }

    /** A finite multiplier above 0. Missing text means the shipped default, 1.0. */
    static double positiveMultiplier(String location, String text, List<ConfigWarning> warnings) {
        if (text == null) {
            return 1.0D;
        }
        double value;
        try {
            value = Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            warnings.add(new ConfigWarning(location, text, "is not a number; using 1.0"));
            return 1.0D;
        }
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0D) {
            warnings.add(new ConfigWarning(location, text, "must be a finite number above 0; using 1.0"));
            return 1.0D;
        }
        return value;
    }

    public static final class Result {

        private final BalanceSettings settings;
        private final List<ConfigWarning> warnings;

        Result(BalanceSettings settings, List<ConfigWarning> warnings) {
            this.settings = settings;
            this.warnings = Collections.unmodifiableList(warnings);
        }

        public BalanceSettings settings() {
            return settings;
        }

        public List<ConfigWarning> warnings() {
            return warnings;
        }
    }
}
