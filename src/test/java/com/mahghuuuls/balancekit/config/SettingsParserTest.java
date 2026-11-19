package com.mahghuuuls.balancekit.config;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsParserTest {

    private static SettingsParser.Result parseMobHealth(String text) {
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.MOBS_HEALTH, text);
        return SettingsParser.parse(raw);
    }

    @Test
    void shippedDefaultIsNeutral() {
        SettingsParser.Result result = SettingsParser.parse(Collections.<String, String>emptyMap());
        assertEquals(1.0D, result.settings().mobHealth());
        assertTrue(result.warnings().isEmpty());
    }

    private static SettingsParser.Result parseRange(String lower, String upper) {
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.VARIATION_LOWER, lower);
        raw.put(SettingsParser.VARIATION_UPPER, upper);
        return SettingsParser.parse(raw);
    }

    @Test
    void variationDefaultsToOneAndAcceptsAValidRange() {
        BalanceSettings neutral = SettingsParser.parse(Collections.<String, String>emptyMap()).settings();
        assertEquals(1.0D, neutral.variationLower());
        assertEquals(1.0D, neutral.variationUpper());
        SettingsParser.Result result = parseRange("1.3", "1.9");
        assertTrue(result.warnings().isEmpty());
        assertEquals(1.3D, result.settings().variationLower());
        assertEquals(1.9D, result.settings().variationUpper());
        assertEquals(1.5D, parseRange("1.5", "1.5").settings().variationUpper());
    }

    @Test
    void lowerAboveUpperWarnsAndUsesOneForBoth() {
        SettingsParser.Result result = parseRange("2", "1.5");
        assertEquals(1, result.warnings().size());
        assertEquals(1.0D, result.settings().variationLower());
        assertEquals(1.0D, result.settings().variationUpper());
    }

    @Test
    void invalidVariationEndWarnsAndUsesOne() {
        SettingsParser.Result result = parseRange("0", "1.5");
        assertEquals(1, result.warnings().size());
        assertEquals("mobs.variationLower", result.warnings().get(0).location());
        assertEquals(1.0D, result.settings().variationLower());
        assertEquals(1.5D, result.settings().variationUpper());
    }

    @Test
    void damageMultipliersAcceptZeroAndRejectNegativeValues() {
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.PLAYER_DEALT, "0");
        raw.put(SettingsParser.PLAYER_RECEIVED, "-1");
        raw.put(SettingsParser.MOBS_DEALT, "2.5");
        raw.put(SettingsParser.MOBS_RECEIVED, "abc");
        SettingsParser.Result result = SettingsParser.parse(raw);
        assertEquals(0.0D, result.settings().playerDealt());
        assertEquals(1.0D, result.settings().playerReceived());
        assertEquals(2.5D, result.settings().mobDealt());
        assertEquals(1.0D, result.settings().mobReceived());
        assertEquals(2, result.warnings().size());
        assertEquals("mobs.damageReceived", result.warnings().get(0).location());
        assertEquals("player.damageReceived", result.warnings().get(1).location());
    }

    @Test
    void everyDamageSettingAcceptsZeroAndRejectsANegativeValue() {
        String[] keys = {SettingsParser.PLAYER_DEALT, SettingsParser.PLAYER_RECEIVED, SettingsParser.MOBS_DEALT,
                SettingsParser.MOBS_RECEIVED};
        for (String key : keys) {
            Map<String, String> raw = new HashMap<>();
            raw.put(key, "0");
            SettingsParser.Result zero = SettingsParser.parse(raw);
            assertTrue(zero.warnings().isEmpty(), key);
            raw.put(key, "-0.5");
            SettingsParser.Result negative = SettingsParser.parse(raw);
            assertEquals(1, negative.warnings().size(), key);
            assertEquals(key, negative.warnings().get(0).location());
        }
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.MOBS_DEALT, "0");
        raw.put(SettingsParser.MOBS_RECEIVED, "0");
        raw.put(SettingsParser.PLAYER_RECEIVED, "0");
        BalanceSettings settings = SettingsParser.parse(raw).settings();
        assertEquals(0.0D, settings.mobDealt());
        assertEquals(0.0D, settings.mobReceived());
        assertEquals(0.0D, settings.playerReceived());
    }

    @Test
    void damageDefaultsAreNeutralAndTheLogIsOff() {
        BalanceSettings settings = SettingsParser.parse(Collections.<String, String>emptyMap()).settings();
        assertEquals(1.0D, settings.playerDealt());
        assertEquals(1.0D, settings.playerReceived());
        assertEquals(1.0D, settings.mobDealt());
        assertEquals(1.0D, settings.mobReceived());
        assertTrue(settings.excludedDamageTypes().isEmpty());
        assertEquals(false, settings.logDamageTypes());
    }

    @Test
    void theLogSwitchReadsTrueOrFalseAndWarnsOtherwise() {
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.LOG_DAMAGE_TYPES, " TRUE ");
        assertEquals(true, SettingsParser.parse(raw).settings().logDamageTypes());
        raw.put(SettingsParser.LOG_DAMAGE_TYPES, "yes");
        SettingsParser.Result result = SettingsParser.parse(raw);
        assertEquals(false, result.settings().logDamageTypes());
        assertEquals(1, result.warnings().size());
        assertEquals("diagnostics.logDamageTypes", result.warnings().get(0).location());
    }

    @Test
    void playerBaseHealthDefaultsOffAt20AndRejectsInvalidValues() {
        BalanceSettings neutral = SettingsParser.parse(Collections.<String, String>emptyMap()).settings();
        assertEquals(false, neutral.playerBaseHealthEnabled());
        assertEquals(20.0D, neutral.playerBaseHealth());
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.PLAYER_BASE_HEALTH_ENABLED, "true");
        raw.put(SettingsParser.PLAYER_BASE_HEALTH, "10");
        SettingsParser.Result ok = SettingsParser.parse(raw);
        assertTrue(ok.warnings().isEmpty());
        assertEquals(true, ok.settings().playerBaseHealthEnabled());
        assertEquals(10.0D, ok.settings().playerBaseHealth());
        for (String bad : new String[] {"0", "-5", "NaN", "abc"}) {
            raw.put(SettingsParser.PLAYER_BASE_HEALTH, bad);
            SettingsParser.Result result = SettingsParser.parse(raw);
            assertEquals(20.0D, result.settings().playerBaseHealth(), bad);
            assertEquals(1, result.warnings().size(), bad);
            assertEquals("player.baseHealth", result.warnings().get(0).location(), bad);
        }
    }

    @Test
    void movementDefaultsToOneAndRejectsZeroOrLess() {
        BalanceSettings neutral = SettingsParser.parse(Collections.<String, String>emptyMap()).settings();
        assertEquals(1.0D, neutral.mobMovement());
        assertEquals(1.0D, neutral.playerMovement());
        assertEquals(1.0D, neutral.playerAttackSpeed());
        for (String key : new String[] {SettingsParser.MOBS_MOVEMENT, SettingsParser.PLAYER_MOVEMENT,
                SettingsParser.PLAYER_ATTACK_SPEED}) {
            Map<String, String> raw = new HashMap<>();
            raw.put(key, "1.5");
            assertTrue(SettingsParser.parse(raw).warnings().isEmpty(), key);
            for (String bad : new String[] {"0", "-1.5"}) {
                raw.put(key, bad);
                SettingsParser.Result result = SettingsParser.parse(raw);
                assertEquals(1, result.warnings().size(), key + " " + bad);
                assertEquals(key, result.warnings().get(0).location());
            }
        }
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.MOBS_MOVEMENT, "1.5");
        raw.put(SettingsParser.PLAYER_MOVEMENT, "1.2");
        raw.put(SettingsParser.PLAYER_ATTACK_SPEED, "2");
        BalanceSettings settings = SettingsParser.parse(raw).settings();
        assertEquals(2.0D, settings.playerAttackSpeed());
        assertEquals(1.5D, settings.mobMovement());
        assertEquals(1.2D, settings.playerMovement());
    }

    @Test
    void acceptsPositiveMultipliers() {
        assertEquals(0.5D, parseMobHealth("0.5").settings().mobHealth());
        assertEquals(2.0D, parseMobHealth(" 2 ").settings().mobHealth());
        assertTrue(parseMobHealth("2").warnings().isEmpty());
    }

    @Test
    void rejectsInvalidValuesWithOneWarningNamingTheSetting() {
        for (String bad : new String[] {"-1", "0", "NaN", "Infinity", "abc"}) {
            SettingsParser.Result result = parseMobHealth(bad);
            assertEquals(1.0D, result.settings().mobHealth(), bad);
            assertEquals(1, result.warnings().size(), bad);
            assertEquals("mobs.health", result.warnings().get(0).location(), bad);
            assertEquals(bad, result.warnings().get(0).text(), bad);
        }
    }
}
