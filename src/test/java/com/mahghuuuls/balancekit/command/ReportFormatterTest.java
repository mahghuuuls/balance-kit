package com.mahghuuuls.balancekit.command;

import com.mahghuuuls.balancekit.mob.HealthReport;
import com.mahghuuuls.balancekit.player.PlayerHealthReport;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportFormatterTest {

    @Test
    void thePlayerHealthLinesShowTheSwitchBasesAndResult() {
        assertEquals(Arrays.asList("Base health: on, 30", "Max health: underlying base 20, adjusted base 30, result 34; health 34"),
                ReportFormatter.playerHealth(new PlayerHealthReport(true, 30.0D, 20.0D, 30.0D, 34.0D, 34.0D, 1024.0D)));
        assertEquals(Arrays.asList("Base health: off", "Max health: underlying base 20, adjusted base 20, result 20; health 15"),
                ReportFormatter.playerHealth(new PlayerHealthReport(false, 30.0D, 20.0D, 20.0D, 20.0D, 15.0D, 1024.0D)));
        assertEquals("Not yet in effect: the config asks for base 30.",
                ReportFormatter.playerHealth(new PlayerHealthReport(true, 30.0D, 25.0D, 37.5D, 37.5D, 37.5D, 1024.0D)).get(2));
        assertEquals("Not yet in effect: the config asks for base 20.",
                ReportFormatter.playerHealth(new PlayerHealthReport(false, 30.0D, 20.0D, 30.0D, 30.0D, 30.0D, 1024.0D)).get(2));
        assertEquals("Cap: the max health cap of 1024 applies.",
                ReportFormatter.playerHealth(new PlayerHealthReport(true, 5000.0D, 20.0D, 5000.0D, 1024.0D, 1024.0D, 1024.0D)).get(2));
    }

    @Test
    void theMovementLineShowsTheMultiplierAndTheBlocklist() {
        assertEquals("Movement: x1.5; speed 0.345", ReportFormatter.movement(1.5D, false, 0.345D));
        assertEquals("Movement: x1 (movement blocklist); speed 0.23", ReportFormatter.movement(1.0D, true, 0.23D));
        assertEquals("Movement: x1.2", ReportFormatter.movement(1.2D, false, Double.NaN));
    }

    @Test
    void theAttackSpeedLineShowsTheMultiplierAndValue() {
        assertEquals("Attack speed: x2; value 3.2; recharge 6.25 ticks", ReportFormatter.attackSpeed(2.0D, 3.2D, 6.25D));
        assertEquals("Attack speed: x1", ReportFormatter.attackSpeed(1.0D, Double.NaN, Double.NaN));
    }

    @Test
    void theDamageLineShowsBothMultipliersAndTheBlocklist() {
        assertEquals("Damage: dealt x2, received x1.5", ReportFormatter.damage(2.0D, 1.5D, false));
        assertEquals("Damage: dealt x1, received x1 (its own multipliers skipped: damage blocklist)",
                ReportFormatter.damage(1.0D, 1.0D, true));
    }

    @Test
    void aMobWithoutARollReadsNotAvailable() {
        List<String> lines = ReportFormatter.health(
                new HealthReport(20.0D, 20.0D, 1.0D, 20.0D, 20.0D, 1024.0D, Double.NaN, 1.0D, false));
        assertEquals("Variation: roll n/a, multiplier x1", lines.get(2));
    }

    @Test
    void numbersDropTrailingZeros() {
        assertEquals("40", ReportFormatter.number(40.0D));
        assertEquals("0.345", ReportFormatter.number(0.345D));
        assertEquals("1.5", ReportFormatter.number(1.5D));
        assertEquals("n/a", ReportFormatter.number(Double.NaN));
    }

    @Test
    void balancedMobShowsUnderlyingAdjustedAndResult() {
        List<String> lines = ReportFormatter.health(new HealthReport(20.0D, 40.0D, 2.0D, 40.0D, 34.0D, 1024.0D, 0.25D, 1.0D, false));
        assertEquals(Arrays.asList("Mob: yes",
                "Max health: underlying base 20, adjusted base 40 (x2), result 40; health 34",
                "Variation: roll 0.25, multiplier x1"), lines);
    }

    @Test
    void cappedResultSaysSo() {
        List<String> lines = ReportFormatter.health(new HealthReport(300.0D, 30000.0D, 100.0D, 1024.0D, 1024.0D, 1024.0D, 0.5D, 1.5D, true));
        assertEquals("Max health: underlying base 300, adjusted base 30000 (x100), result 1024; health 1024", lines.get(1));
        assertEquals("Variation: skipped (blocklisted); roll 0.5", lines.get(2));
        assertEquals("Cap: the max health cap of 1024 applies.", lines.get(3));
    }

    @Test
    void pendingMobSaysSo() {
        assertEquals(Arrays.asList("Mob: yes; first balance pending (it happens on the mob's first server tick)."),
                ReportFormatter.health(HealthReport.pending()));
    }

    @Test
    void anAdjustmentNotYetInEffectIsShownAsApplied() {
        // A tamed wolf: base 20, Balance Kit's stale share of 8 still applied, config asks for x2.
        List<String> lines = ReportFormatter.health(new HealthReport(20.0D, 28.0D, 2.0D, 28.0D, 20.0D, 1024.0D, 0.0D, 1.0D, false));
        assertEquals("Max health: underlying base 20, adjusted base 28 (x1.4), result 28; health 20", lines.get(1));
        assertEquals("Not yet in effect: the config asks for x2.", lines.get(2));
    }

    @Test
    void headerWithoutANameHasNoQuotes() {
        assertEquals("Balance Kit: minecraft:cow (entity 3)",
                ReportFormatter.report("minecraft:cow", null, 3, Arrays.<String>asList()).get(0));
    }

    @Test
    void headerNamesTheEntityAndIndentsSections() {
        List<String> lines = ReportFormatter.report("minecraft:zombie", "bk_zombie", 7, Arrays.asList("Mob: yes"));
        assertEquals("Balance Kit: minecraft:zombie \"bk_zombie\" (entity 7)", lines.get(0));
        assertEquals("  Mob: yes", lines.get(1));
    }
}
