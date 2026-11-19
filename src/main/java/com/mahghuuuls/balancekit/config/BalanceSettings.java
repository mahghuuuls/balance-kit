package com.mahghuuuls.balancekit.config;

import java.util.Collections;
import java.util.List;

/**
 * The validated config values, read once at startup. Every value here is already checked; 1.0 means no change.
 */
public final class BalanceSettings {

    public static final BalanceSettings NEUTRAL = new BalanceSettings(1.0D, 1.0D, 1.0D, Collections.<EntrySpec>emptyList(),
            Collections.<EntrySpec>emptyList(), Collections.<EntrySpec>emptyList(), Collections.<EntrySpec>emptyList(),
            Collections.<EntrySpec>emptyList());

    private final double mobHealth;
    private final double variationLower;
    private final double variationUpper;
    private final List<EntrySpec> entityRules;
    private final List<EntrySpec> healthBlocklist;
    private final List<EntrySpec> variationBlocklist;
    private final List<EntrySpec> damageBlocklist;
    private final List<EntrySpec> movementBlocklist;

    public BalanceSettings(double mobHealth, double variationLower, double variationUpper,
                           List<EntrySpec> entityRules, List<EntrySpec> healthBlocklist,
                           List<EntrySpec> variationBlocklist, List<EntrySpec> damageBlocklist,
                           List<EntrySpec> movementBlocklist) {
        this.mobHealth = mobHealth;
        this.variationLower = variationLower;
        this.variationUpper = variationUpper;
        this.entityRules = Collections.unmodifiableList(entityRules);
        this.healthBlocklist = Collections.unmodifiableList(healthBlocklist);
        this.variationBlocklist = Collections.unmodifiableList(variationBlocklist);
        this.damageBlocklist = Collections.unmodifiableList(damageBlocklist);
        this.movementBlocklist = Collections.unmodifiableList(movementBlocklist);
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
}
