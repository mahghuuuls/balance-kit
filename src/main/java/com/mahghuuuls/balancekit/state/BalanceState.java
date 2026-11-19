package com.mahghuuuls.balancekit.state;

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
 * and its health fraction at save time (computed by the caller that attached the state). Transient: values that only
 * matter while the entity is loaded.
 */
public final class BalanceState {

    static final int VERSION = 1;
    static final String KEY_VERSION = "v";
    static final String KEY_BALANCED = "balanced";
    static final String KEY_HEALTH_FRACTION = "healthFraction";
    static final String KEY_MATCHES = "matches";
    private static final int TAG_STRING = 8;

    /** No saved fraction. */
    public static final float NO_FRACTION = -1.0F;

    private boolean balanced;
    private Set<String> matches = Collections.emptySet();
    private float savedHealthFraction = NO_FRACTION;
    private double lastHealthBase = Double.NaN;
    private float healthAfterLastApply = Float.NaN;

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

    /** The fraction saved with the entity, or {@link #NO_FRACTION}. Returned once, right after a load. */
    public float takeSavedHealthFraction() {
        float fraction = savedHealthFraction;
        savedHealthFraction = NO_FRACTION;
        return fraction;
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
        balanced = tag.getBoolean(KEY_BALANCED);
        savedHealthFraction = tag.hasKey(KEY_HEALTH_FRACTION) ? tag.getFloat(KEY_HEALTH_FRACTION) : NO_FRACTION;
        Set<String> read = new TreeSet<>();
        NBTTagList list = tag.getTagList(KEY_MATCHES, TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) {
            read.add(list.getStringTagAt(i));
        }
        matches = Collections.unmodifiableSet(read);
    }
}
