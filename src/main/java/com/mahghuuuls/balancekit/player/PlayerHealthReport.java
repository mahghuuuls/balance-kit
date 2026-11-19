package com.mahghuuuls.balancekit.player;

/** One player's max health as Balance Kit sees it, for the inspection report. */
public final class PlayerHealthReport {

    private final boolean enabled;
    private final double configuredBase;
    private final double underlyingBase;
    private final double adjustedBase;
    private final double maxHealth;
    private final double health;
    private final double cap;

    public PlayerHealthReport(boolean enabled, double configuredBase, double underlyingBase, double adjustedBase,
                              double maxHealth, double health, double cap) {
        this.enabled = enabled;
        this.configuredBase = configuredBase;
        this.underlyingBase = underlyingBase;
        this.adjustedBase = adjustedBase;
        this.maxHealth = maxHealth;
        this.health = health;
        this.cap = cap;
    }

    /** Whether the player base health switch is on. */
    public boolean enabled() {
        return enabled;
    }

    /** The configured base value (used only while the switch is on). */
    public double configuredBase() {
        return configuredBase;
    }

    public double underlyingBase() {
        return underlyingBase;
    }

    /** The base max health is computed from: the underlying base plus Balance Kit's share. */
    public double adjustedBase() {
        return adjustedBase;
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
        return !Double.isNaN(cap) && maxHealth >= cap;
    }

    /** True when the applied base is what the config asks for; false until the next recheck after a base change. */
    public boolean inEffect() {
        return !enabled ? adjustedBase == underlyingBase : Math.abs(adjustedBase - configuredBase) < 1.0E-6D;
    }
}
