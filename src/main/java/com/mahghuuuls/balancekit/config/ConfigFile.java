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
