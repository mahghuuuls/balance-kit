package com.mahghuuuls.balancekit.state;

import com.mahghuuuls.balancekit.rules.MobProfile;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Balance Kit's own record on one entity. This is the only data Balance Kit saves: its attribute changes are never
 * saved, so they are rebuilt from this record and the current config every time the entity loads.
 *
 * <p>Saved: whether the entity had its first balance, the fingerprints of the conditioned entries it matched then,
 * its variation roll (drawn at first balance and kept for life), and its health fraction at save time (computed by
 * the caller that attached the state). Transient: values that only matter while the entity is loaded.
 */
public final class BalanceState {

    static final int VERSION = 1;
    static final String KEY_VERSION = "v";
    static final String KEY_BALANCED = "balanced";
    static final String KEY_HEALTH_FRACTION = "healthFraction";
    static final String KEY_MATCHES = "matches";
    static final String KEY_ROLL = "roll";
    private static final int TAG_STRING = 8;

    /** No saved fraction. */
    public static final float NO_FRACTION = -1.0F;

    private boolean balanced;
    private Set<String> matches = Collections.emptySet();
    private double roll = Double.NaN;
    private float savedHealthFraction = NO_FRACTION;
    private double lastHealthBase = Double.NaN;
    private MobProfile cachedProfile;
    private float healthAfterLastApply = Float.NaN;
    private float healthToRestore = Float.NaN;

    public boolean isBalanced() {
        return balanced;
    }

    public void markBalanced() {
        balanced = true;
    }

    /** Fingerprints of the conditioned entries matched at first balance; empty when none. */
    public Set<String> matches() {
        return matches;
    }

    public void setMatches(Set<String> fingerprints) {
        matches = Collections.unmodifiableSet(new TreeSet<>(fingerprints));
    }

    /** The mob's variation roll in [0, 1), kept for life; NaN before it has one. */
    public double roll() {
        return roll;
    }

    public void setRoll(double roll) {
        this.roll = roll;
    }

    /**
     * Health a respawned player must get back after its max health is set: the game copies health into the new player
     * entity at the End exit before Balance Kit's share exists, which clamps it. NaN when none.
     */
    public float takeHealthToRestore() {
        float health = healthToRestore;
        healthToRestore = Float.NaN;
        return health;
    }

    public void setHealthToRestore(float health) {
        this.healthToRestore = health;
    }

    /** Copies the saved part of another state, as when the game replaces a player entity on respawn. */
    public void copySavedFrom(BalanceState other) {
        read(other.write(NO_FRACTION));
    }

    /** The fraction saved with the entity, or {@link #NO_FRACTION}. Returned once, right after a load. */
    public float takeSavedHealthFraction() {
        float fraction = savedHealthFraction;
        savedHealthFraction = NO_FRACTION;
        return fraction;
    }

    /**
     * The profile resolved for this mob, kept while it is loaded: its matches and roll do not change after the first
     * balance and the config is read once. Reading saved data (a load, or an operator's edit) clears it.
     */
    public MobProfile cachedProfile() {
        return cachedProfile;
    }

    public void setCachedProfile(MobProfile profile) {
        this.cachedProfile = profile;
    }

    public double lastHealthBase() {
        return lastHealthBase;
    }

    public void setLastHealthBase(double base) {
        lastHealthBase = base;
    }

    /** Health at the previous check of the entity in this load (adjustment or recheck), or NaN. */
    public float healthAfterLastApply() {
        return healthAfterLastApply;
    }

    public void setHealthAfterLastApply(float health) {
        healthAfterLastApply = health;
    }

    NBTTagCompound write(float currentHealthFraction) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(KEY_VERSION, VERSION);
        tag.setBoolean(KEY_BALANCED, balanced);
        if (currentHealthFraction > 0.0F) {
            tag.setFloat(KEY_HEALTH_FRACTION, currentHealthFraction);
        }
        if (!Double.isNaN(roll)) {
            tag.setDouble(KEY_ROLL, roll);
        }
        if (!matches.isEmpty()) {
            NBTTagList list = new NBTTagList();
            for (String fingerprint : matches) {
                list.appendTag(new NBTTagString(fingerprint));
            }
            tag.setTag(KEY_MATCHES, list);
        }
        return tag;
    }

    void read(NBTTagCompound tag) {
        cachedProfile = null;
        balanced = tag.getBoolean(KEY_BALANCED);
        savedHealthFraction = tag.hasKey(KEY_HEALTH_FRACTION) ? tag.getFloat(KEY_HEALTH_FRACTION) : NO_FRACTION;
        roll = tag.hasKey(KEY_ROLL) ? tag.getDouble(KEY_ROLL) : Double.NaN;
        Set<String> read = new TreeSet<>();
        NBTTagList list = tag.getTagList(KEY_MATCHES, TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) {
            read.add(list.getStringTagAt(i));
        }
        matches = Collections.unmodifiableSet(read);
    }
}
