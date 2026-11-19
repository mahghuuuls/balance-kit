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
