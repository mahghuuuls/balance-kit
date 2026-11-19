package com.mahghuuuls.balancekit.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The validated config values, read once at startup. Every value here is already checked; 1.0 means no change.
 * Only {@link SettingsParser} builds one, so a value cannot skip validation.
 */
public final class BalanceSettings {

    public static final BalanceSettings NEUTRAL = new Builder().build();

    private final double mobHealth;
    private final double variationLower;
    private final double variationUpper;
    private final double mobDealt;
    private final double mobReceived;
    private final double playerDealt;
    private final double playerReceived;
    private final boolean playerBaseHealthEnabled;
    private final double playerBaseHealth;
    private final Set<String> excludedDamageTypes;
    private final boolean logDamageTypes;
    private final List<EntrySpec> entityRules;
    private final List<EntrySpec> healthBlocklist;
    private final List<EntrySpec> variationBlocklist;
    private final List<EntrySpec> damageBlocklist;
    private final List<EntrySpec> movementBlocklist;

    private BalanceSettings(Builder b) {
        this.mobHealth = b.mobHealth;
        this.variationLower = b.variationLower;
        this.variationUpper = b.variationUpper;
        this.mobDealt = b.mobDealt;
        this.mobReceived = b.mobReceived;
        this.playerDealt = b.playerDealt;
        this.playerReceived = b.playerReceived;
        this.playerBaseHealthEnabled = b.playerBaseHealthEnabled;
        this.playerBaseHealth = b.playerBaseHealth;
        this.excludedDamageTypes = Collections.unmodifiableSet(new LinkedHashSet<>(b.excludedDamageTypes));
        this.logDamageTypes = b.logDamageTypes;
        this.entityRules = Collections.unmodifiableList(new ArrayList<>(b.entityRules));
        this.healthBlocklist = Collections.unmodifiableList(new ArrayList<>(b.healthBlocklist));
        this.variationBlocklist = Collections.unmodifiableList(new ArrayList<>(b.variationBlocklist));
        this.damageBlocklist = Collections.unmodifiableList(new ArrayList<>(b.damageBlocklist));
        this.movementBlocklist = Collections.unmodifiableList(new ArrayList<>(b.movementBlocklist));
    }

    /** Global max health multiplier for every mob. */
    public double mobHealth() {
        return mobHealth;
    }

    /** Lower end of the global health variation range; never above {@link #variationUpper()}. */
    public double variationLower() {
        return variationLower;
    }

    public double variationUpper() {
        return variationUpper;
    }

    /** Global damage-dealt multiplier for every mob; 0 or more. */
    public double mobDealt() {
        return mobDealt;
    }

    /** Global damage-received multiplier for every mob; 0 or more. */
    public double mobReceived() {
        return mobReceived;
    }

    /** Damage-dealt multiplier for every player; 0 or more. */
    public double playerDealt() {
        return playerDealt;
    }

    /** Damage-received multiplier for every player; 0 or more. */
    public double playerReceived() {
        return playerReceived;
    }

    /** Whether every player's max health is computed from {@link #playerBaseHealth()} as its base. */
    public boolean playerBaseHealthEnabled() {
        return playerBaseHealthEnabled;
    }

    /** The base max health for players while the switch is on; above 0. */
    public double playerBaseHealth() {
        return playerBaseHealth;
    }

    /** Exact damage type names that are never scaled. */
    public Set<String> excludedDamageTypes() {
        return excludedDamageTypes;
    }

    /** Whether each damage event writes one log line. */
    public boolean logDamageTypes() {
        return logDamageTypes;
    }

    public List<EntrySpec> entityRules() {
        return entityRules;
    }

    public List<EntrySpec> healthBlocklist() {
        return healthBlocklist;
    }

    public List<EntrySpec> variationBlocklist() {
        return variationBlocklist;
    }

    public List<EntrySpec> damageBlocklist() {
        return damageBlocklist;
    }

    public List<EntrySpec> movementBlocklist() {
        return movementBlocklist;
    }

    /** Collects validated values; starts at the shipped defaults. */
    static final class Builder {
        double mobHealth = 1.0D;
        double variationLower = 1.0D;
        double variationUpper = 1.0D;
        double mobDealt = 1.0D;
        double mobReceived = 1.0D;
        double playerDealt = 1.0D;
        double playerReceived = 1.0D;
        boolean playerBaseHealthEnabled;
        double playerBaseHealth = 20.0D;
        Set<String> excludedDamageTypes = Collections.emptySet();
        boolean logDamageTypes;
        List<EntrySpec> entityRules = Collections.emptyList();
        List<EntrySpec> healthBlocklist = Collections.emptyList();
        List<EntrySpec> variationBlocklist = Collections.emptyList();
        List<EntrySpec> damageBlocklist = Collections.emptyList();
        List<EntrySpec> movementBlocklist = Collections.emptyList();

        BalanceSettings build() {
            return new BalanceSettings(this);
        }
    }
}
