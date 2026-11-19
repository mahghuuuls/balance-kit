package com.mahghuuuls.balancekit.rules;

import com.mahghuuuls.balancekit.config.BalanceSettings;
import com.mahghuuuls.balancekit.config.ConfigWarning;
import com.mahghuuuls.balancekit.config.SettingsParser;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Builds the rule book from config text through the real parser, so the syntax and the policy are tested together. */
class RuleBookTest {

    private static RuleBook book(String globalHealth, String[] rules, String[] healthBlock, String[] variationBlock) {
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.MOBS_HEALTH, globalHealth);
        Map<String, String[]> lists = new HashMap<>();
        lists.put(SettingsParser.ENTITY_RULES, rules);
        lists.put(SettingsParser.HEALTH_BLOCKLIST, healthBlock);
        lists.put(SettingsParser.VARIATION_BLOCKLIST, variationBlock);
        SettingsParser.Result result = SettingsParser.parse(raw, lists);
        for (ConfigWarning warning : result.warnings()) {
            throw new AssertionError(warning.toString());
        }
        return new RuleBook(result.settings());
    }

    private static RuleBook book(String globalHealth, String... rules) {
        return book(globalHealth, rules, new String[0], new String[0]);
    }

    private static NBTTagCompound nbt(String text) {
        try {
            return JsonToNBT.getTagFromJson(text);
        } catch (NBTException e) {
            throw new AssertionError(e);
        }
    }

    private static final Set<String> NONE = Collections.emptySet();
    private static final double NO_ROLL = Double.NaN;

    private static RuleBook varied(String globalHealth, String lower, String upper, String[] healthBlock,
                                   String[] variationBlock) {
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.MOBS_HEALTH, globalHealth);
        raw.put(SettingsParser.VARIATION_LOWER, lower);
        raw.put(SettingsParser.VARIATION_UPPER, upper);
        Map<String, String[]> lists = new HashMap<>();
        lists.put(SettingsParser.HEALTH_BLOCKLIST, healthBlock);
        lists.put(SettingsParser.VARIATION_BLOCKLIST, variationBlock);
        SettingsParser.Result result = SettingsParser.parse(raw, lists);
        assertTrue(result.warnings().isEmpty(), result.warnings().toString());
        return new RuleBook(result.settings());
    }

    @Test
    void mobDamageIsTheGlobalTimesEveryMatchingRuleAndTheBlocklistSkipsIt() {
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.MOBS_DEALT, "2");
        raw.put(SettingsParser.MOBS_RECEIVED, "0.5");
        Map<String, String[]> lists = new HashMap<>();
        lists.put(SettingsParser.ENTITY_RULES, new String[] {"minecraft:zombie dealt=1.5 received=3",
                "minecraft:zombie dealt=2"});
        lists.put(SettingsParser.DAMAGE_BLOCKLIST, new String[] {"minecraft:wither"});
        RuleBook book = new RuleBook(SettingsParser.parse(raw, lists).settings());
        MobProfile zombie = book.profile("minecraft:zombie", NONE, NO_ROLL);
        assertEquals(6.0D, zombie.dealt(), 1.0E-9);
        assertEquals(1.5D, zombie.received(), 1.0E-9);
        MobProfile cow = book.profile("minecraft:cow", NONE, NO_ROLL);
        assertEquals(2.0D, cow.dealt(), 1.0E-9);
        assertEquals(0.5D, cow.received(), 1.0E-9);
        MobProfile wither = book.profile("minecraft:wither", NONE, NO_ROLL);
        assertEquals(1.0D, wither.dealt(), 1.0E-9);
        assertEquals(1.0D, wither.received(), 1.0E-9);
        assertTrue(wither.damageBlocked());
    }

    @Test
    void mobMovementIsTheGlobalTimesEveryMatchingRuleAndTheBlocklistSkipsIt() {
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.MOBS_MOVEMENT, "1.5");
        Map<String, String[]> lists = new HashMap<>();
        lists.put(SettingsParser.ENTITY_RULES, new String[] {"minecraft:spider movement=2"});
        lists.put(SettingsParser.MOVEMENT_BLOCKLIST, new String[] {"minecraft:zombie"});
        RuleBook book = new RuleBook(SettingsParser.parse(raw, lists).settings());
        assertEquals(3.0D, book.profile("minecraft:spider", NONE, NO_ROLL).movement(), 1.0E-9);
        assertEquals(1.5D, book.profile("minecraft:cow", NONE, NO_ROLL).movement(), 1.0E-9);
        assertEquals(1.0D, book.profile("minecraft:zombie", NONE, NO_ROLL).movement(), 1.0E-9);
        assertTrue(book.profile("minecraft:zombie", NONE, NO_ROLL).movementBlocked());
    }

    @Test
    void variationMultipliesTheHealthFactor() {
        MobProfile zombie = varied("2", "1.0", "2.0", new String[0], new String[0])
                .profile("minecraft:zombie", NONE, 0.5D);
        assertEquals(1.5D, zombie.variationMultiplier(), 1.0E-9);
        assertEquals(3.0D, zombie.healthFactor(), 1.0E-9);
    }

    @Test
    void withoutARollThereIsNoVariation() {
        MobProfile zombie = varied("2", "1.0", "2.0", new String[0], new String[0])
                .profile("minecraft:zombie", NONE, NO_ROLL);
        assertEquals(1.0D, zombie.variationMultiplier(), 1.0E-9);
        assertEquals(2.0D, zombie.healthFactor(), 1.0E-9);
    }

    @Test
    void variationBlocklistKeepsHealthButSkipsVariation() {
        RuleBook book = varied("2", "1.5", "1.5", new String[0], new String[] {"minecraft:wither"});
        MobProfile wither = book.profile("minecraft:wither", NONE, 0.9D);
        assertEquals(2.0D, wither.healthFactor(), 1.0E-9);
        assertEquals(1.0D, wither.variationMultiplier(), 1.0E-9);
        assertTrue(wither.variationBlocked());
        assertEquals(3.0D, book.profile("minecraft:zombie", NONE, 0.9D).healthFactor(), 1.0E-9);
    }

    @Test
    void healthBlocklistSkipsVariationToo() {
        MobProfile wither = varied("2", "1.5", "1.5", new String[] {"minecraft:wither"}, new String[0])
                .profile("minecraft:wither", NONE, 0.9D);
        assertEquals(1.0D, wither.healthFactor(), 1.0E-9);
        assertEquals(1.0D, wither.variationMultiplier(), 1.0E-9);
        assertTrue(wither.variationBlocked());
    }

    @Test
    void globalTimesEveryMatchingRuleInAnyOrder() {
        assertEquals(3.6D, book("2", "minecraft:zombie health=1.5", "minecraft:zombie health=1.2")
                .profile("minecraft:zombie", NONE, NO_ROLL).healthFactor(), 1.0E-9);
        assertEquals(3.6D, book("2", "minecraft:zombie health=1.2", "minecraft:zombie health=1.5")
                .profile("minecraft:zombie", NONE, NO_ROLL).healthFactor(), 1.0E-9);
        assertEquals(2.0D, book("2", "minecraft:zombie health=1.5").profile("minecraft:cow", NONE, NO_ROLL).healthFactor(), 1.0E-9);
    }

    @Test
    void unsetMultipliersAreNeutral() {
        MobProfile profile = book("1", "minecraft:zombie health=1.5").profile("minecraft:zombie", NONE, NO_ROLL);
        assertEquals(1.0D, profile.dealt());
        assertEquals(1.0D, profile.received());
        assertEquals(1.0D, profile.movement());
    }

    @Test
    void damageAndMovementBlocklistsResolveInsideTheRuleBook() {
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.MOBS_HEALTH, "1");
        Map<String, String[]> lists = new HashMap<>();
        lists.put(SettingsParser.ENTITY_RULES, new String[] {"minecraft:zombie dealt=2 received=3 movement=1.5"});
        lists.put(SettingsParser.DAMAGE_BLOCKLIST, new String[] {"minecraft:zombie {IsBaby:1b}"});
        RuleBook rules = new RuleBook(SettingsParser.parse(raw, lists).settings());
        MobProfile adult = rules.profile("minecraft:zombie", NONE, NO_ROLL);
        assertEquals(2.0D, adult.dealt(), 1.0E-9);
        assertEquals(3.0D, adult.received(), 1.0E-9);
        assertEquals(1.5D, adult.movement(), 1.0E-9);
        MobProfile baby = rules.profile("minecraft:zombie",
                rules.evaluateFirst("minecraft:zombie", () -> nbt("{IsBaby:1b}")), NO_ROLL);
        assertEquals(1.0D, baby.dealt(), 1.0E-9);
        assertEquals(1.0D, baby.received(), 1.0E-9);
        assertEquals(1.5D, baby.movement(), 1.0E-9);
    }

    @Test
    void aMatchSavedForARuleDoesNotApplyANewBlocklistEntryWithTheSameCondition() {
        RuleBook before = book("2", "minecraft:wither {Invul:0} health=3");
        Set<String> matches = before.evaluateFirst("minecraft:wither", () -> nbt("{Invul:0}"));
        RuleBook after = book("2", new String[0], new String[] {"minecraft:wither {Invul:0}"}, new String[0]);
        MobProfile wither = after.profile("minecraft:wither", matches, NO_ROLL);
        assertFalse(wither.healthBlocked());
        assertEquals(2.0D, wither.healthFactor(), 1.0E-9);
    }

    @Test
    void keysThatNeedQuotingKeepDistinctFingerprints() {
        // Quoting keeps a key with separators from reading as several keys: unquoted, the key "a:1,b" with value 1
        // would print as {a:1,b:1}, the same as the two keys a and b.
        assertEquals("{\"a,b\":1}", RuleBook.canonical(nbt("{\"a,b\":1}")));
        assertEquals("{a:1,b:1}", RuleBook.canonical(nbt("{b:1,a:1}")));
    }

    @Test
    void movementBlocklistLeavesMovementAtOne() {
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.MOBS_HEALTH, "1");
        Map<String, String[]> lists = new HashMap<>();
        lists.put(SettingsParser.ENTITY_RULES, new String[] {"minecraft:bat movement=2 dealt=2"});
        lists.put(SettingsParser.MOVEMENT_BLOCKLIST, new String[] {"minecraft:bat"});
        MobProfile bat = new RuleBook(SettingsParser.parse(raw, lists).settings()).profile("minecraft:bat", NONE, NO_ROLL);
        assertEquals(1.0D, bat.movement(), 1.0E-9);
        assertEquals(2.0D, bat.dealt(), 1.0E-9);
        assertTrue(bat.movementBlocked());
    }

    @Test
    void listOrderInAConditionDoesNotChangeTheFingerprint() {
        Set<String> matches = book("1", "minecraft:zombie {Tags:[\"a\",\"b\"]} health=3")
                .evaluateFirst("minecraft:zombie", () -> nbt("{Tags:[\"a\",\"b\",\"c\"]}"));
        assertEquals(3.0D, book("1", "minecraft:zombie {Tags:[\"b\",\"a\"]} health=3")
                .profile("minecraft:zombie", matches, NO_ROLL).healthFactor(), 1.0E-9);
    }

    @Test
    void conditionsAreCheckedOnceAndOnlyWhenNeeded() {
        RuleBook rules = book("1", "minecraft:zombie {IsBaby:1b} health=3", "minecraft:cow health=2");
        AtomicInteger reads = new AtomicInteger();
        Set<String> baby = rules.evaluateFirst("minecraft:zombie", () -> {
            reads.incrementAndGet();
            return nbt("{IsBaby:1b,Health:20f}");
        });
        Set<String> adult = rules.evaluateFirst("minecraft:zombie", () -> nbt("{IsBaby:0b}"));
        rules.evaluateFirst("minecraft:cow", () -> {
            throw new AssertionError("no conditioned entry for cows, so their NBT is never read");
        });
        assertEquals(1, reads.get());
        assertEquals(3.0D, rules.profile("minecraft:zombie", baby, NO_ROLL).healthFactor(), 1.0E-9);
        assertEquals(1.0D, rules.profile("minecraft:zombie", adult, NO_ROLL).healthFactor(), 1.0E-9);
    }

    @Test
    void numberTypesMustMatchAsWritten() {
        RuleBook rules = book("1", "minecraft:zombie {IsBaby:1b} health=3");
        assertTrue(rules.evaluateFirst("minecraft:zombie", () -> nbt("{IsBaby:1}")).isEmpty());
    }

    @Test
    void savedMatchesSurviveReorderingButNotAnEditedCondition() {
        RuleBook before = book("1", "minecraft:cow health=2", "minecraft:zombie {IsBaby:1b} health=3");
        Set<String> matches = before.evaluateFirst("minecraft:zombie", () -> nbt("{IsBaby:1b}"));
        RuleBook reordered = book("1", "minecraft:zombie {IsBaby:1b} health=3", "minecraft:cow health=2");
        assertEquals(3.0D, reordered.profile("minecraft:zombie", matches, NO_ROLL).healthFactor(), 1.0E-9);
        RuleBook edited = book("1", "minecraft:zombie {IsBaby:1b,CanBreakDoors:0b} health=3");
        assertEquals(1.0D, edited.profile("minecraft:zombie", matches, NO_ROLL).healthFactor(), 1.0E-9);
        RuleBook deleted = book("1", "minecraft:cow health=2");
        assertEquals(1.0D, deleted.profile("minecraft:zombie", matches, NO_ROLL).healthFactor(), 1.0E-9);
    }

    @Test
    void conditionKeyOrderDoesNotChangeTheFingerprint() {
        Set<String> matches = book("1", "minecraft:zombie {IsBaby:1b,CanBreakDoors:0b} health=3")
                .evaluateFirst("minecraft:zombie", () -> nbt("{IsBaby:1b,CanBreakDoors:0b}"));
        assertEquals(3.0D, book("1", "minecraft:zombie {CanBreakDoors:0b, IsBaby:1b} health=3")
                .profile("minecraft:zombie", matches, NO_ROLL).healthFactor(), 1.0E-9);
    }

    @Test
    void unconditionedRulesFollowTheCurrentConfig() {
        assertEquals(3.0D, book("1", "minecraft:zombie health=3").profile("minecraft:zombie", NONE, NO_ROLL).healthFactor(), 1.0E-9);
    }

    @Test
    void healthBlocklistWinsAndAlsoSkipsVariation() {
        RuleBook rules = book("2", new String[] {"minecraft:wither health=5"}, new String[] {"minecraft:wither"},
                new String[0]);
        MobProfile wither = rules.profile("minecraft:wither", NONE, NO_ROLL);
        assertEquals(1.0D, wither.healthFactor(), 1.0E-9);
        assertTrue(wither.healthBlocked());
        assertTrue(wither.variationBlocked());
        assertEquals(2, wither.appliedEntries().size());
    }

    @Test
    void variationBlocklistKeepsHealthMultipliers() {
        RuleBook rules = book("2", new String[0], new String[0], new String[] {"minecraft:wither"});
        MobProfile wither = rules.profile("minecraft:wither", NONE, NO_ROLL);
        assertEquals(2.0D, wither.healthFactor(), 1.0E-9);
        assertFalse(wither.healthBlocked());
        assertTrue(wither.variationBlocked());
    }

    @Test
    void appliedEntriesNameTheirListAndLine() {
        MobProfile profile = book("1", "minecraft:cow health=2", "minecraft:zombie health=3").profile("minecraft:zombie", NONE, NO_ROLL);
        assertEquals(Collections.singletonList("rules.entityRules line 2: minecraft:zombie health=3"),
                profile.appliedEntries());
    }
}
