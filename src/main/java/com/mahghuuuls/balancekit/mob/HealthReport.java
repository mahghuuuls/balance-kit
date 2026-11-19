package com.mahghuuuls.balancekit.mob;

/**
 * The max health part of an inspection: values only; the command formats them. The adjusted base is the one
 * actually applied, so the report never claims an adjustment that is not in effect.
 */
public final class HealthReport {

    private final boolean balanced;
    private final double underlyingBase;
    private final double adjustedBase;
    private final double configuredFactor;
    private final double maxHealth;
    private final double health;
    private final double cap;

    public HealthReport(double underlyingBase, double adjustedBase, double configuredFactor, double maxHealth,
                        double health, double cap) {
        this.balanced = true;
        this.underlyingBase = underlyingBase;
        this.adjustedBase = adjustedBase;
        this.configuredFactor = configuredFactor;
        this.maxHealth = maxHealth;
        this.health = health;
        this.cap = cap;
    }

    private HealthReport() {
        this.balanced = false;
        this.underlyingBase = Double.NaN;
        this.adjustedBase = Double.NaN;
        this.configuredFactor = Double.NaN;
        this.maxHealth = Double.NaN;
        this.health = Double.NaN;
        this.cap = Double.NaN;
    }

    public static HealthReport pending() {
        return new HealthReport();
    }

    /** False until the mob's first balance, which happens on its first server tick. */
    public boolean balanced() {
        return balanced;
    }

    public double underlyingBase() {
        return underlyingBase;
    }

    /** The base max health is computed from right now, as applied (see Contribution.appliedMaxHealthBase). */
    public double adjustedBase() {
        return adjustedBase;
    }

    /** The factor in effect right now. */
    public double appliedFactor() {
        return underlyingBase == 0.0D ? 1.0D : adjustedBase() / underlyingBase;
    }

    /** The factor the config asks for; differs from {@link #appliedFactor()} until the next recheck. */
    public double configuredFactor() {
        return configuredFactor;
    }

    public boolean inEffect() {
        return Math.abs(appliedFactor() - configuredFactor) < 1.0E-6D;
    }

    public double maxHealth() {
        return maxHealth;
    }

    public double health() {
        return health;
    }

    public double cap() {
        return cap;
    }

    /** True when the result sits at the attribute's cap. */
    public boolean capped() {
        return maxHealth >= cap - 1.0E-6D;
    }
}
