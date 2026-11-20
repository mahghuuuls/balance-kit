package com.mahghuuuls.balancekit.breathing;

/**
 * Turns the air a player lost in one tick into the air it should lose with breathing multiplier k: the observed loss
 * divided by k, kept as a fractional balance so the long-run average has no drift. One instance per player.
 */
public final class AirLossScaler {

    /** Absorbs floating-point error so a balance of 0.9999999 after three thirds still counts as one. */
    private static final double EPSILON = 1.0E-9D;

    private final double multiplier;
    private double balance;

    /** {@code multiplier} is k, above 0: air lasts k times as long. */
    public AirLossScaler(double multiplier) {
        this.multiplier = multiplier;
    }

    /**
     * The air to set after a tick that went from {@code before} to {@code after} underwater. Acts only when air was
     * above 0 at the tick start and fell; the result is never above {@code before} and never below 0, so the game's
     * drowning countdown below 0 is never touched. Returns {@code after} when there is nothing to scale.
     */
    public int adjustedAir(int before, int after) {
        if (before <= 0 || after >= before) {
            return after;
        }
        return Math.max(0, before - scaledLoss(before - after));
    }

    /** The whole air to take for an observed loss of {@code observedLoss} (0 or more); the rest is kept for later. */
    int scaledLoss(int observedLoss) {
        balance += observedLoss / multiplier;
        int whole = (int) Math.floor(balance + EPSILON);
        balance = Math.max(0.0D, balance - whole);
        return whole;
    }
}
