package com.mahghuuuls.balancekit.state;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void noFractionIsWrittenWhenThereIsNone() {
        BalanceState state = new BalanceState();
        assertFalse(state.write(BalanceState.NO_FRACTION).hasKey("healthFraction"));
        assertFalse(new BalanceState().isBalanced());
    }
}
