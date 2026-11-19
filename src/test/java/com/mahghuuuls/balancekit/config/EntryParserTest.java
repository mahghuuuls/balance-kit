package com.mahghuuuls.balancekit.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryParserTest {

    private static List<EntrySpec> rules(List<ConfigWarning> warnings, String... lines) {
        return EntryParser.parse("rules.entityRules", lines, true, warnings);
    }

    @Test
    void readsIdConditionAndMultipliers() {
        List<ConfigWarning> warnings = new ArrayList<>();
        EntrySpec spec = rules(warnings, "minecraft:zombie {IsBaby:1b} health=1.5 dealt=2").get(0);
        assertTrue(warnings.isEmpty(), warnings.toString());
        assertEquals("minecraft:zombie", spec.entityId());
        assertNotNull(spec.condition());
        assertEquals(1, spec.condition().getByte("IsBaby"));
        assertEquals(1.5D, spec.health());
        assertEquals(2.0D, spec.dealt());
        assertTrue(Double.isNaN(spec.received()));
        assertTrue(Double.isNaN(spec.movement()));
        assertEquals(1, spec.line());
    }

    @Test
    void bareIdsAreMinecraftAndCaseIsIgnored() {
        List<ConfigWarning> warnings = new ArrayList<>();
        EntrySpec spec = rules(warnings, "Zombie health=2").get(0);
        assertEquals("minecraft:zombie", spec.entityId());
        assertNull(spec.condition());
    }

    @Test
    void bracesInsideQuotedStringsDoNotEndTheCondition() {
        List<ConfigWarning> warnings = new ArrayList<>();
        EntrySpec spec = rules(warnings, "minecraft:zombie{CustomName:\"a } b\"} received=0").get(0);
        assertTrue(warnings.isEmpty(), warnings.toString());
        assertEquals("a } b", spec.condition().getString("CustomName"));
        assertEquals(0.0D, spec.received());
    }

    @Test
    void badLinesAreSkippedWithTheirPositionAndOthersStillApply() {
        List<ConfigWarning> warnings = new ArrayList<>();
        List<EntrySpec> specs = rules(warnings,
                "minecraft:zombie {IsBaby:1b health=2",
                "",
                "minecraft:cow health=2",
                "minecraft:pig health=0",
                "minecraft:pig dealt=-1",
                "minecraft:pig speed=2",
                "minecraft:pig health=2 health=3",
                "minecraft:pig health=abc",
                "{IsBaby:1b} health=2");
        assertEquals(1, specs.size());
        assertEquals("minecraft:cow", specs.get(0).entityId());
        assertEquals(3, specs.get(0).line());
        assertEquals(7, warnings.size(), warnings.toString());
        assertEquals("rules.entityRules line 1", warnings.get(0).location());
        assertEquals("minecraft:zombie {IsBaby:1b health=2", warnings.get(0).text());
    }

    @Test
    void blocklistLinesTakeNoMultipliers() {
        List<ConfigWarning> warnings = new ArrayList<>();
        List<EntrySpec> specs = EntryParser.parse("blocklists.health",
                new String[] {"minecraft:wither", "minecraft:wither health=2"}, false, warnings);
        assertEquals(1, specs.size());
        assertEquals(1, warnings.size());
        assertEquals("blocklists.health line 2", warnings.get(0).location());
    }
}
