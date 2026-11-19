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
    public static final String VARIATION_LOWER = "mobs.variationLower";
    public static final String VARIATION_UPPER = "mobs.variationUpper";
    public static final String ENTITY_RULES = "rules.entityRules";
    public static final String HEALTH_BLOCKLIST = "blocklists.health";
    public static final String VARIATION_BLOCKLIST = "blocklists.variation";
    public static final String DAMAGE_BLOCKLIST = "blocklists.damage";
    public static final String MOVEMENT_BLOCKLIST = "blocklists.movement";

    private SettingsParser() {
    }

    public static Result parse(Map<String, String> raw) {
        return parse(raw, Collections.<String, String[]>emptyMap());
    }

    /** {@code raw} holds single values by key, {@code lists} the lines of each list setting by key. */
    public static Result parse(Map<String, String> raw, Map<String, String[]> lists) {
        List<ConfigWarning> warnings = new ArrayList<>();
        double mobHealth = positiveMultiplier(MOBS_HEALTH, raw.get(MOBS_HEALTH), warnings);
        double lower = positiveMultiplier(VARIATION_LOWER, raw.get(VARIATION_LOWER), warnings);
        double upper = positiveMultiplier(VARIATION_UPPER, raw.get(VARIATION_UPPER), warnings);
        if (lower > upper) {
            warnings.add(new ConfigWarning(VARIATION_LOWER + " and " + VARIATION_UPPER, lower + " and " + upper,
                    "lower is above upper; using 1.0 for both"));
            lower = 1.0D;
            upper = 1.0D;
        }
        BalanceSettings settings = new BalanceSettings(mobHealth, lower, upper,
                EntryParser.parse(ENTITY_RULES, lines(lists, ENTITY_RULES), true, warnings),
                EntryParser.parse(HEALTH_BLOCKLIST, lines(lists, HEALTH_BLOCKLIST), false, warnings),
                EntryParser.parse(VARIATION_BLOCKLIST, lines(lists, VARIATION_BLOCKLIST), false, warnings),
                EntryParser.parse(DAMAGE_BLOCKLIST, lines(lists, DAMAGE_BLOCKLIST), false, warnings),
                EntryParser.parse(MOVEMENT_BLOCKLIST, lines(lists, MOVEMENT_BLOCKLIST), false, warnings));
        return new Result(settings, warnings);
    }

    private static String[] lines(Map<String, String[]> lists, String key) {
        String[] lines = lists.get(key);
        return lines == null ? new String[0] : lines;
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
