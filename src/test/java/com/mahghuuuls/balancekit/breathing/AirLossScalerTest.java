package com.mahghuuuls.balancekit.breathing;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirLossScalerTest {

    private static int total(double k, int ticks) {
        AirLossScaler scaler = new AirLossScaler(k);
        int sum = 0;
        for (int i = 0; i < ticks; i++) {
            int loss = scaler.scaledLoss(1);
            assertTrue(loss >= 0);
            sum += loss;
        }
        return sum;
    }

    @Test
    void longRunAverageIsTheLossDividedByK() {
        for (double k : new double[] {0.5D, 1.0D, 1.5D, 2.0D, 3.0D}) {
            int sum = total(k, 10000);
            double expected = 10000 / k;
            assertEquals(expected, sum, expected * 0.005D, "k " + k);
        }
    }

    @Test
    void exactCyclesHaveNoDrift() {
        assertEquals(5000, total(2.0D, 10000));
        assertEquals(3333, total(3.0D, 10000));
        assertEquals(20000, total(0.5D, 10000));
        assertEquals(6666, total(1.5D, 10000));
    }

    @Test
    void respirationKeepsItsBenefitMultipliedByK() {
        Random random = new Random(7);
        AirLossScaler scaler = new AirLossScaler(2.0D);
        int observed = 0;
        int scaled = 0;
        for (int i = 0; i < 100000; i++) {
            int loss = random.nextInt(4) > 0 ? 0 : 1;
            observed += loss;
            scaled += scaler.scaledLoss(loss);
        }
        assertEquals(observed / 2.0D, scaled, 1.0D);
    }

    @Test
    void adjustedAirActsOnlyOnALossAboveZeroAndStaysWithinZeroAndTheTickStart() {
        AirLossScaler half = new AirLossScaler(2.0D);
        assertEquals(300, half.adjustedAir(300, 299), "half a loss: kept for later");
        assertEquals(299, half.adjustedAir(300, 299), "the second half: one air");
        assertEquals(260, half.adjustedAir(250, 260), "air rose: the game's value stays");
        assertEquals(250, half.adjustedAir(250, 250), "no loss: unchanged");
        assertEquals(-1, half.adjustedAir(0, -1), "at 0 the drowning countdown is the game's");
        assertEquals(-5, half.adjustedAir(-4, -5));
        AirLossScaler fast = new AirLossScaler(0.5D);
        assertEquals(0, fast.adjustedAir(1, 0), "a scaled loss of 2 from 1 stops at 0, not -1");
        assertEquals(298, fast.adjustedAir(300, 299));
    }

    @Test
    void noLossTakesNothing() {
        AirLossScaler scaler = new AirLossScaler(2.0D);
        assertEquals(0, scaler.scaledLoss(0));
        assertEquals(0, scaler.scaledLoss(1));
        assertEquals(1, scaler.scaledLoss(1));
    }
}
