package com.mahghuuuls.balancekit.rules;

import java.util.Collections;
import java.util.List;

/**
 * What applies to one mob, fully resolved by {@link RuleBook}: each factor is the global multiplier times every
 * applicable rule's multiplier, or 1 when the matching blocklist applies. Callers use the factors as they are.
 */
public final class MobProfile {

    private final double healthFactor;
    private final double variationMultiplier;
    private final double dealt;
    private final double received;
    private final double movement;
    private final boolean healthBlocked;
    private final boolean variationBlocked;
    private final boolean damageBlocked;
    private final boolean movementBlocked;
    private final List<String> appliedEntries;

    MobProfile(double healthFactor, double variationMultiplier, double dealt, double received, double movement, boolean healthBlocked,
               boolean variationBlocked, boolean damageBlocked, boolean movementBlocked, List<String> appliedEntries) {
        this.healthFactor = healthFactor;
        this.variationMultiplier = variationMultiplier;
        this.dealt = dealt;
        this.received = received;
        this.movement = movement;
        this.healthBlocked = healthBlocked;
        this.variationBlocked = variationBlocked;
        this.damageBlocked = damageBlocked;
        this.movementBlocked = movementBlocked;
        this.appliedEntries = Collections.unmodifiableList(appliedEntries);
    }

    /**
     * Global mob health multiplier times every applicable rule's health multiplier times the variation multiplier;
     * 1 when health-blocklisted.
     */
    public double healthFactor() {
        return healthFactor;
    }

    /** The variation multiplier included in {@link #healthFactor()}; 1 when blocklisted or without a roll. */
    public double variationMultiplier() {
        return variationMultiplier;
    }

    /** Damage-dealt factor; 1 when damage-blocklisted. */
    public double dealt() {
        return dealt;
    }

    /** Damage-received factor; 1 when damage-blocklisted. */
    public double received() {
        return received;
    }

    /** Movement factor; 1 when movement-blocklisted. */
    public double movement() {
        return movement;
    }

    public boolean healthBlocked() {
        return healthBlocked;
    }

    /** True for the variation blocklist and for the health blocklist, which also skips variation. */
    public boolean variationBlocked() {
        return variationBlocked;
    }

    public boolean damageBlocked() {
        return damageBlocked;
    }

    public boolean movementBlocked() {
        return movementBlocked;
    }

    /** Every applicable rule and blocklist entry, as "list line n: text", in config order. */
    public List<String> appliedEntries() {
        return appliedEntries;
    }
}
