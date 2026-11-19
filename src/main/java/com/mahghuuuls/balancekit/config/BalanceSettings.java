package com.mahghuuuls.balancekit.config;

/**
 * The validated config values, read once at startup. Every value here is already checked; 1.0 means no change.
 */
public final class BalanceSettings {

    public static final BalanceSettings NEUTRAL = new BalanceSettings(1.0D);

    private final double mobHealth;

    public BalanceSettings(double mobHealth) {
        this.mobHealth = mobHealth;
    }

    /** Global max health multiplier for every mob. */
    public double mobHealth() {
        return mobHealth;
    }
}
