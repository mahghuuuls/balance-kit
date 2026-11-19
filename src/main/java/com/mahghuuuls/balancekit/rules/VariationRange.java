package com.mahghuuuls.balancekit.rules;

/**
 * The global health variation range. A mob's roll is its relative position in [0, 1); the multiplier is
 * {@code lower + roll x (upper - lower)}, so the upper value itself is reached only when both are equal, and a new
 * range maps the same roll to the same relative position.
 */
public final class VariationRange {

    private final double lower;
    private final double upper;

    public VariationRange(double lower, double upper) {
        this.lower = lower;
        this.upper = upper;
    }

    public double multiplier(double roll) {
        if (lower == upper) {
            return lower;
        }
        // Rounding can carry a roll just below 1 up to the upper value, and a saved roll edited outside [0, 1) would
        // leave the range; keep the result at or above lower and below upper.
        double value = lower + roll * (upper - lower);
        return Math.max(lower, Math.min(value, Math.nextDown(upper)));
    }
}
