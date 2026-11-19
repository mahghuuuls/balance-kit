package com.mahghuuuls.balancekit.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariationRangeTest {

    @Test
    void mapsTheRollLinearlyAndNeverReachesTheUpperValue() {
        VariationRange range = new VariationRange(1.3D, 1.9D);
        assertEquals(1.3D, range.multiplier(0.0D), 1.0E-12);
        assertEquals(1.6D, range.multiplier(0.5D), 1.0E-12);
        assertTrue(range.multiplier(Math.nextDown(1.0D)) < 1.9D);
    }

    @Test
    void aNewRangeKeepsTheSameRelativePlace() {
        assertEquals(1.5D, new VariationRange(1.0D, 2.0D).multiplier(0.5D), 1.0E-12);
        assertEquals(3.0D, new VariationRange(2.0D, 4.0D).multiplier(0.5D), 1.0E-12);
    }

    @Test
    void equalEndsGiveThatValue() {
        assertEquals(1.5D, new VariationRange(1.5D, 1.5D).multiplier(0.73D), 1.0E-12);
    }

    @Test
    void aRollOutsideTheUnitRangeStaysInTheRange() {
        VariationRange range = new VariationRange(1.3D, 1.9D);
        assertEquals(1.3D, range.multiplier(-0.5D), 1.0E-12);
        assertTrue(range.multiplier(1.5D) < 1.9D);
    }
}
