package com.mahghuuuls.balancekit.attribute;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * The opt-in Scaling Health cleanup. Scaling Health 1.3.42 saves two modifiers with players and mobs: one on max health
 * and one on attack damage. When it is removed from a pack they stay in the saves. When active, this removes exactly
 * those two modifiers (by their ids); every other modifier stays. Balance Kit never removes another mod's modifier
 * anywhere else.
 *
 * <p>It does not set health. It returns the health fraction the entity had before the removal; {@link Contribution},
 * which owns how health is kept when max health changes, runs it and sets health in the same call.
 */
public final class LegacyCleanup {

    static final UUID SCALING_HEALTH_HEALTH_ID = UUID.fromString("c0bef565-35f6-4dc5-bb4c-3644c382e6ce");
    static final UUID SCALING_HEALTH_DAMAGE_ID = UUID.fromString("d3560b15-c459-451c-86a8-0247015ae899");

    /** No cleanup. */
    public static final LegacyCleanup OFF = new LegacyCleanup(false);

    private static final Logger LOG = LogManager.getLogger("Balance Kit");

    private final boolean active;
    private boolean firstRemovalLogged;

    private LegacyCleanup(boolean active) {
        this.active = active;
    }

    /**
     * The cleanup for this game: active only when the switch is on and Scaling Health is not installed. With the
     * switch on and Scaling Health installed, it logs one warning and stays off.
     */
    public static LegacyCleanup create(boolean switchOn, boolean scalingHealthInstalled) {
        if (!switchOn) {
            return OFF;
        }
        if (scalingHealthInstalled) {
            LOG.warn("Config: compat.removeScalingHealthModifiers is true, but Scaling Health is installed;"
                    + " nothing is removed while it is installed");
            return OFF;
        }
        LOG.info("Config: Scaling Health cleanup is on; its leftover modifiers are removed from players at login"
                + " and from mobs at their next balance");
        return new LegacyCleanup(true);
    }

    /**
     * Removes Scaling Health's two modifiers from the entity. Returns the health fraction the entity had before the
     * max health modifier was removed, or {@link Contribution#NO_FRACTION} when no max health modifier was removed
     * (or the cleanup is off).
     */
    public float remove(EntityLivingBase entity) {
        if (!active) {
            return Contribution.NO_FRACTION;
        }
        Result result = removeFrom(entity.getAttributeMap(), entity.getHealth());
        if (result.removedAny) {
            if (!firstRemovalLogged) {
                firstRemovalLogged = true;
                LOG.info("Scaling Health cleanup: removed its leftover modifiers from {}; later removals are logged at"
                        + " debug level", entity.getName());
            } else {
                LOG.debug("Scaling Health cleanup: removed its leftover modifiers from {}", entity.getName());
            }
        }
        return result.fractionBefore;
    }

    /**
     * Removes the two modifiers from the map when active. {@code health} is the entity's current health; the
     * returned fraction is taken against the max health before the removal.
     */
    Result removeFrom(AbstractAttributeMap map, float health) {
        if (!active) {
            return Result.NOTHING;
        }
        IAttributeInstance maxHealth = map.getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH);
        double maxBefore = maxHealth == null ? Double.NaN : maxHealth.getAttributeValue();
        boolean healthRemoved = remove(maxHealth, SCALING_HEALTH_HEALTH_ID);
        boolean damageRemoved = remove(map.getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE),
                SCALING_HEALTH_DAMAGE_ID);
        float fraction = healthRemoved && maxBefore > 0.0D ? (float) (health / maxBefore) : Contribution.NO_FRACTION;
        return new Result(healthRemoved || damageRemoved, fraction);
    }

    private static boolean remove(IAttributeInstance instance, UUID id) {
        if (instance == null) {
            return false;
        }
        AttributeModifier modifier = instance.getModifier(id);
        if (modifier == null) {
            return false;
        }
        instance.removeModifier(modifier);
        return true;
    }

    static final class Result {
        static final Result NOTHING = new Result(false, Contribution.NO_FRACTION);

        final boolean removedAny;
        final float fractionBefore;

        Result(boolean removedAny, float fractionBefore) {
            this.removedAny = removedAny;
            this.fractionBefore = fractionBefore;
        }
    }
}
