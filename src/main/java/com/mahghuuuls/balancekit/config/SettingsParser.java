package com.mahghuuuls.balancekit.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns the raw text of each setting into validated values. Owns the accepted ranges; an invalid value is reported
 * and replaced by its neutral value, never silently repaired to something else.
 */
public final class SettingsParser {

    public static final String MOBS_HEALTH = "mobs.health";
    public static final String VARIATION_LOWER = "mobs.variationLower";
    public static final String VARIATION_UPPER = "mobs.variationUpper";
    public static final String MOBS_DEALT = "mobs.damageDealt";
    public static final String MOBS_RECEIVED = "mobs.damageReceived";
    public static final String PLAYER_DEALT = "player.damageDealt";
    public static final String PLAYER_RECEIVED = "player.damageReceived";
    public static final String MOBS_MOVEMENT = "mobs.movementSpeed";
    public static final String PLAYER_MOVEMENT = "player.movementSpeed";
    public static final String PLAYER_ATTACK_SPEED = "player.attackSpeed";
    public static final String PLAYER_BREATHING = "player.breathingDuration";
    public static final String PLAYER_BASE_HEALTH_ENABLED = "player.baseHealthEnabled";
    public static final String PLAYER_BASE_HEALTH = "player.baseHealth";
    public static final String EXCLUDED_DAMAGE_TYPES = "damage.excludedDamageTypes";
    public static final String LOG_DAMAGE_TYPES = "diagnostics.logDamageTypes";
    public static final String REMOVE_SCALING_HEALTH = "compat.removeScalingHealthModifiers";
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
        BalanceSettings.Builder b = new BalanceSettings.Builder();
        b.mobHealth = mobHealth;
        b.variationLower = lower;
        b.variationUpper = upper;
        b.mobDealt = nonNegativeMultiplier(MOBS_DEALT, raw.get(MOBS_DEALT), warnings);
        b.mobReceived = nonNegativeMultiplier(MOBS_RECEIVED, raw.get(MOBS_RECEIVED), warnings);
        b.playerDealt = nonNegativeMultiplier(PLAYER_DEALT, raw.get(PLAYER_DEALT), warnings);
        b.playerReceived = nonNegativeMultiplier(PLAYER_RECEIVED, raw.get(PLAYER_RECEIVED), warnings);
        b.mobMovement = positiveMultiplier(MOBS_MOVEMENT, raw.get(MOBS_MOVEMENT), warnings);
        b.playerMovement = positiveMultiplier(PLAYER_MOVEMENT, raw.get(PLAYER_MOVEMENT), warnings);
        b.playerAttackSpeed = positiveMultiplier(PLAYER_ATTACK_SPEED, raw.get(PLAYER_ATTACK_SPEED), warnings);
        b.playerBreathing = positiveMultiplier(PLAYER_BREATHING, raw.get(PLAYER_BREATHING), warnings);
        b.playerBaseHealthEnabled = switchSetting(PLAYER_BASE_HEALTH_ENABLED, raw.get(PLAYER_BASE_HEALTH_ENABLED), warnings);
        b.playerBaseHealth = positiveValue(PLAYER_BASE_HEALTH, raw.get(PLAYER_BASE_HEALTH), 20.0D, warnings);
        b.excludedDamageTypes = names(lines(lists, EXCLUDED_DAMAGE_TYPES));
        b.logDamageTypes = switchSetting(LOG_DAMAGE_TYPES, raw.get(LOG_DAMAGE_TYPES), warnings);
        b.removeScalingHealthModifiers = switchSetting(REMOVE_SCALING_HEALTH, raw.get(REMOVE_SCALING_HEALTH), warnings);
        b.entityRules = EntryParser.parse(ENTITY_RULES, lines(lists, ENTITY_RULES), true, warnings);
        b.healthBlocklist = EntryParser.parse(HEALTH_BLOCKLIST, lines(lists, HEALTH_BLOCKLIST), false, warnings);
        b.variationBlocklist = EntryParser.parse(VARIATION_BLOCKLIST, lines(lists, VARIATION_BLOCKLIST), false, warnings);
        b.damageBlocklist = EntryParser.parse(DAMAGE_BLOCKLIST, lines(lists, DAMAGE_BLOCKLIST), false, warnings);
        b.movementBlocklist = EntryParser.parse(MOVEMENT_BLOCKLIST, lines(lists, MOVEMENT_BLOCKLIST), false, warnings);
        return new Result(b.build(), warnings);
    }

    /** List lines as exact names: surrounding spaces and empty lines dropped, nothing else changed. */
    private static Set<String> names(String[] lines) {
        Set<String> names = new LinkedHashSet<>();
        for (String line : lines) {
            String name = line.trim();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    /** A switch: true or false, in any letter case. Missing text means the shipped default, false. */
    static boolean switchSetting(String location, String text, List<ConfigWarning> warnings) {
        if (text == null) {
            return false;
        }
        String value = text.trim();
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (!value.equalsIgnoreCase("false")) {
            warnings.add(new ConfigWarning(location, text, "must be true or false; using false"));
        }
        return false;
    }

    /** A finite multiplier of 0 or more. Missing text means the shipped default, 1.0. */
    static double nonNegativeMultiplier(String location, String text, List<ConfigWarning> warnings) {
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
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0D) {
            warnings.add(new ConfigWarning(location, text, "must be a finite number of 0 or more; using 1.0"));
            return 1.0D;
        }
        return value;
    }

    private static String[] lines(Map<String, String[]> lists, String key) {
        String[] lines = lists.get(key);
        return lines == null ? new String[0] : lines;
    }

    /** A finite multiplier above 0. Missing text means the shipped default, 1.0. */
    static double positiveMultiplier(String location, String text, List<ConfigWarning> warnings) {
        return positiveValue(location, text, 1.0D, warnings);
    }

    /** A finite number above 0. Missing or invalid text gives {@code fallback}, the shipped default. */
    static double positiveValue(String location, String text, double fallback, List<ConfigWarning> warnings) {
        if (text == null) {
            return fallback;
        }
        double value;
        try {
            value = Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            warnings.add(new ConfigWarning(location, text, "is not a number; using " + fallback));
            return fallback;
        }
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0D) {
            warnings.add(new ConfigWarning(location, text, "must be a finite number above 0; using " + fallback));
            return fallback;
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
