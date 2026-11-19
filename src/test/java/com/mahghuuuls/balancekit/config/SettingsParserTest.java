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
