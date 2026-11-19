package com.mahghuuuls.balancekit.config;

import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

/**
 * One parsed config list line: an entity rule or a blocklist entry. A multiplier the line does not set is NaN.
 * The condition is shared, not copied; callers must not change it.
 */
public final class EntrySpec {

    private final String list;
    private final int line;
    private final String text;
    private final String entityId;
    @Nullable
    private final NBTTagCompound condition;
    private final double health;
    private final double dealt;
    private final double received;
    private final double movement;

    EntrySpec(String list, int line, String text, String entityId, @Nullable NBTTagCompound condition,
              double health, double dealt, double received, double movement) {
        this.list = list;
        this.line = line;
        this.text = text;
        this.entityId = entityId;
        this.condition = condition;
        this.health = health;
        this.dealt = dealt;
        this.received = received;
        this.movement = movement;
    }

    /** The setting the line came from, for example {@code rules.entityRules}. */
    public String list() {
        return list;
    }

    /** 1-based position in that list. */
    public int line() {
        return line;
    }

    /** The line as written, trimmed. */
    public String text() {
        return text;
    }

    /** Namespaced entity ID, for example {@code minecraft:zombie}. */
    public String entityId() {
        return entityId;
    }

    @Nullable
    public NBTTagCompound condition() {
        return condition;
    }

    public double health() {
        return health;
    }

    public double dealt() {
        return dealt;
    }

    public double received() {
        return received;
    }

    public double movement() {
        return movement;
    }
}
