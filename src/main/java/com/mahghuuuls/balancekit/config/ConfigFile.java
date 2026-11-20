package com.mahghuuuls.balancekit.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Owns the layout and comments of {@code config/balancekit.cfg}. Reads the raw text of every setting and hands it to
 * {@link SettingsParser}; the file is read once, at startup.
 */
public final class ConfigFile {

    private static final String RESTART = " Restart the game or server to apply changes.";

    private ConfigFile() {
    }

    /**
     * Reads a number setting as text. Forge's own number getter replaces text it cannot read with the default and
     * saves that back without a word; reading text leaves the value as written, so the parser can report it.
     */
    private static String number(Configuration config, String category, String key, String defaultValue, String comment) {
        return config.get(category, key, defaultValue, comment + " Default: " + defaultValue + "." + RESTART,
                Property.Type.DOUBLE).getString();
    }

    /** Reads a true/false setting as text, for the same reason as {@link #number}. */
    private static String switchText(Configuration config, String category, String key, String comment) {
        return config.get(category, key, "false", comment + " Default: false." + RESTART, Property.Type.BOOLEAN)
                .getString();
    }

    private static String[] list(Configuration config, String category, String key, String comment) {
        return config.get(category, key, new String[0], comment + RESTART).getStringList();
    }

    public static BalanceSettings load(File file, Logger log) {
        Configuration config = new Configuration(file);
        config.load();

        config.getCategory("mobs").setComment("Multipliers for mobs: every non-player creature, vanilla or modded. 1.0 means no change.");
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.MOBS_HEALTH, number(config, "mobs", "health", "1.0",
                "Max health multiplier for every mob. Example: 2.0 doubles it. Must be above 0."));
        raw.put(SettingsParser.VARIATION_LOWER, number(config, "mobs", "variationLower", "1.0",
                "Lowest health variation multiplier. Each mob gets a random lasting position between lower and upper."
                        + " Example: 0.8. Must be above 0 and not above variationUpper."));
        raw.put(SettingsParser.VARIATION_UPPER, number(config, "mobs", "variationUpper", "1.0",
                "Highest health variation multiplier (not reached unless equal to lower). Example: 1.2. Must be above 0."));
        raw.put(SettingsParser.MOBS_MOVEMENT, number(config, "mobs", "movementSpeed", "1.0",
                "Movement speed multiplier for every mob. Example: 1.2. Must be above 0. Mobs that fly, swim or move by"
                        + " their own rules may not change speed."));
        raw.put(SettingsParser.MOBS_DEALT, number(config, "mobs", "damageDealt", "1.0",
                "Multiplier for damage every mob deals, before the victim's armor. Example: 1.5. Must be 0 or more."));
        raw.put(SettingsParser.MOBS_RECEIVED, number(config, "mobs", "damageReceived", "1.0",
                "Multiplier for damage every mob takes, before its armor. 0 means hits remove no health. Must be 0 or more."));

        config.getCategory("player").setComment("Settings for players. Fake players (automation stand-ins) get none. 1.0 means no change.");
        raw.put(SettingsParser.PLAYER_BASE_HEALTH_ENABLED, switchText(config, "player", "baseHealthEnabled",
                "true makes every player's max health start from baseHealth instead of the game's base (20 in vanilla). Armor, food, effects and"
                        + " other mods still raise or lower it from there. false leaves players at the game's base."
                        + " true or false."));
        raw.put(SettingsParser.PLAYER_BASE_HEALTH, number(config, "player", "baseHealth", "20.0",
                "Base max health for players while baseHealthEnabled is true. 20.0 is ten hearts. Must be above 0;"
                        + " the game's max health cap still applies."));
        raw.put(SettingsParser.PLAYER_MOVEMENT, number(config, "player", "movementSpeed", "1.0",
                "Walking speed multiplier for every player; sprinting and effects still add their share. Example: 1.2."
                        + " Must be above 0."));
        raw.put(SettingsParser.PLAYER_ATTACK_SPEED, number(config, "player", "attackSpeed", "1.0",
                "Multiplier for every player's final attack speed, after the weapon. 2.0 halves the time a swing takes"
                        + " to recharge. The bow is not affected. Must be above 0."));
        raw.put(SettingsParser.PLAYER_BREATHING, number(config, "player", "breathingDuration", "1.0",
                "How many times as long a player's air lasts underwater. 2.0 doubles it; Respiration keeps its benefit on"
                        + " top. Water Breathing, air recovery and drowning damage are unchanged. Must be above 0."));
        raw.put(SettingsParser.PLAYER_DEALT, number(config, "player", "damageDealt", "1.0",
                "Multiplier for damage every player deals, before the victim's armor. Example: 2.0. Must be 0 or more."));
        raw.put(SettingsParser.PLAYER_RECEIVED, number(config, "player", "damageReceived", "1.0",
                "Multiplier for damage every player takes, before armor. The void and /kill are never scaled. Must be 0 or more."));

        config.getCategory("diagnostics").setComment("Diagnostics for pack authors.");
        raw.put(SettingsParser.LOG_DAMAGE_TYPES, switchText(config, "diagnostics", "logDamageTypes",
                "true writes one server log line for every damage event on a player or mob: damage type, attacker,"
                        + " victim, and the multipliers or the reason none applied. The log grows fast; use it to"
                        + " find damage type names, then turn it off. true or false."));

        config.getCategory("rules").setComment("Entity rules, one per line: <entity id> [<NBT condition>] [health=<n>] [dealt=<n>] [received=<n>] [movement=<n>]."
                + " Example: minecraft:zombie {IsBaby:1b} health=1.5. health and movement must be above 0; dealt and"
                + " received must be 0 or more. Every matching rule multiplies with the global value."
                + " An NBT condition uses /summon syntax and is checked once, when the mob is first balanced.");
        config.getCategory("blocklists").setComment("Blocklists, one entity per line: <entity id> [<NBT condition>]. A listed mob skips that adjustment.");
        Map<String, String[]> lists = new HashMap<>();
        lists.put(SettingsParser.ENTITY_RULES, list(config, "rules", "entityRules",
                "Rules for single mob types or variants. Multipliers not set stay 1.0. Default: empty."));
        lists.put(SettingsParser.HEALTH_BLOCKLIST, list(config, "blocklists", "health",
                "Mobs that get no Balance Kit health change at all (no multipliers, no variation). Example: minecraft:wither. Default: empty."));
        lists.put(SettingsParser.VARIATION_BLOCKLIST, list(config, "blocklists", "variation",
                "Mobs that get health multipliers but no random variation. Default: empty."));
        lists.put(SettingsParser.DAMAGE_BLOCKLIST, list(config, "blocklists", "damage",
                "Mobs whose own damage dealt and received multipliers are skipped. Default: empty."));
        lists.put(SettingsParser.MOVEMENT_BLOCKLIST, list(config, "blocklists", "movement",
                "Mobs that get no movement speed change. Default: empty."));
        config.getCategory("damage").setComment("Damage scaling exceptions.");
        lists.put(SettingsParser.EXCLUDED_DAMAGE_TYPES, list(config, "damage", "excludedDamageTypes",
                "Damage type names that are never scaled, one per line, exact spelling. Vanilla names: inFire,"
                        + " lightningBolt, onFire, lava, hotFloor, inWall, cramming, drown, starve, cactus, fall,"
                        + " flyIntoWall, generic, magic, wither, anvil, fallingBlock, dragonBreath, fireworks, mob,"
                        + " player, arrow, thrown, indirectMagic, thorns, explosion, explosion.player, fireball."
                        + " outOfWorld (the void and /kill) is never scaled anyway. Modded damage has its own names;"
                        + " diagnostics.logDamageTypes shows them. Default: empty."));

        if (config.hasChanged()) {
            config.save();
        }

        SettingsParser.Result result = SettingsParser.parse(raw, lists);
        for (ConfigWarning warning : result.warnings()) {
            log.warn("Config {}", warning);
        }
        return result.settings();
    }
}
