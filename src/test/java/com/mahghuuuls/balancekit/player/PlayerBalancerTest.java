package com.mahghuuuls.balancekit.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerBalancerTest {

    @Test
    void theFactorMakesTheBaseEqualTheConfiguredValue() {
        assertEquals(1.5D, PlayerBalancer.factor(true, 30.0D, 20.0D), 1.0E-12);
        assertEquals(0.5D, PlayerBalancer.factor(true, 10.0D, 20.0D), 1.0E-12);
        assertEquals(1.0D, PlayerBalancer.factor(true, 20.0D, 20.0D), 1.0E-12);
    }

    @Test
    void theSwitchOffOrAnUnusableBaseMeansNoShare() {
        assertEquals(1.0D, PlayerBalancer.factor(false, 30.0D, 20.0D), 1.0E-12);
        assertEquals(1.0D, PlayerBalancer.factor(true, 30.0D, 0.0D), 1.0E-12);
        assertEquals(1.0D, PlayerBalancer.factor(true, 30.0D, Double.NaN), 1.0E-12);
    }
}
