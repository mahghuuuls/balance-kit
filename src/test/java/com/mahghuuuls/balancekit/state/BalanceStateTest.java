package com.mahghuuuls.balancekit.state;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Uses the literal saved key names, so renaming a key (which would break existing saves) fails here. */
class BalanceStateTest {

    @Test
    void writesTheSavedShape() {
        BalanceState state = new BalanceState();
        state.markBalanced();
        NBTTagCompound tag = state.write(0.75F);
        assertEquals(1, tag.getInteger("v"));
        assertTrue(tag.getBoolean("balanced"));
        assertEquals(0.75F, tag.getFloat("healthFraction"));
        assertEquals(3, tag.getKeySet().size());
    }

    @Test
    void readsTheSavedShapeAndGivesTheFractionOnce() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("v", 1);
        tag.setBoolean("balanced", true);
        tag.setFloat("healthFraction", 0.75F);
        BalanceState loaded = new BalanceState();
        loaded.read(tag);
        assertTrue(loaded.isBalanced());
        assertEquals(0.75F, loaded.takeSavedHealthFraction());
        assertEquals(BalanceState.NO_FRACTION, loaded.takeSavedHealthFraction());
    }

    @Test
    void matchesAreSavedAsAStringListAndReadBack() {
        BalanceState state = new BalanceState();
        state.markBalanced();
        state.setMatches(new java.util.TreeSet<>(java.util.Arrays.asList("b", "a")));
        NBTTagCompound tag = state.write(1.0F);
        assertEquals(2, tag.getTagList("matches", 8).tagCount());
        assertEquals("a", tag.getTagList("matches", 8).getStringTagAt(0));
        BalanceState loaded = new BalanceState();
        loaded.read(tag);
        assertEquals(state.matches(), loaded.matches());
    }

    @Test
    void rollIsSavedAndReadBack() {
        BalanceState state = new BalanceState();
        state.markBalanced();
        state.setRoll(0.1D);
        NBTTagCompound tag = state.write(1.0F);
        assertEquals(6, tag.getTagId("roll"), "a double tag");
        assertEquals(0.1D, tag.getDouble("roll"));
        BalanceState loaded = new BalanceState();
        loaded.read(tag);
        assertEquals(0.1D, loaded.roll());
    }

    @Test
    void readingSavedDataClearsTheCachedProfile() {
        BalanceState state = new BalanceState();
        state.markBalanced();
        state.setCachedProfile(new com.mahghuuuls.balancekit.rules.RuleBook(
                com.mahghuuuls.balancekit.config.BalanceSettings.NEUTRAL).profile("minecraft:zombie",
                java.util.Collections.<String>emptySet(), Double.NaN));
        state.read(state.write(1.0F));
        assertNull(state.cachedProfile());
    }

    @Test
    void noRollIsWrittenWhenThereIsNone() {
        BalanceState state = new BalanceState();
        assertFalse(state.write(1.0F).hasKey("roll"));
        BalanceState loaded = new BalanceState();
        loaded.read(new NBTTagCompound());
        assertTrue(Double.isNaN(loaded.roll()));
    }

    @Test
    void noFractionIsWrittenWhenThereIsNone() {
        BalanceState state = new BalanceState();
        assertFalse(state.write(BalanceState.NO_FRACTION).hasKey("healthFraction"));
        assertFalse(new BalanceState().isBalanced());
    }
}
