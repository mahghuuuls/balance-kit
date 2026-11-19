package com.mahghuuuls.balancekit.attribute;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

import java.util.UUID;

/**
 * Applies Balance Kit's share of an attribute, and owns how health is kept when max health changes.
 *
 * <p>A factor is applied as one unsaved add-operation modifier of {@code base x (factor - 1)}. Add modifiers are
 * summed before multiply modifiers, so the result equals a base of {@code base x factor} while other mods' modifiers
 * stack on it as usual. The modifier is never saved, so the entity's saved base stays the underlying base and
 * removing Balance Kit leaves nothing behind; callers reapply it whenever the entity loads.
 */
public final class Contribution {

    /** Marks "no saved fraction"; any value outside (0, 1] means the same. */
    public static final float NO_FRACTION = -1.0F;

    static final UUID MAX_HEALTH_ID = UUID.fromString("0177bbe7-1dd8-4702-bd96-a43fc4abd90a");
    static final String MAX_HEALTH_NAME = "balancekit.maxHealth";
    static final UUID MOVEMENT_ID = UUID.fromString("5c1e3f2a-7b6d-4e9a-9c0f-2d8b1a4e6f73");
    static final String MOVEMENT_NAME = "balancekit.movementSpeed";
    static final int OPERATION_ADD = 0;

    private static final float SAME_HEALTH = 1.0E-3F;

    private Contribution() {
    }

    /**
     * Sets the max health factor, keeping the health percentage the entity had (see {@link #fractionToKeep}).
     * {@code healthAfterLastApply} is the health at the previous check of this entity, or NaN.
     */
    public static boolean applyMaxHealth(EntityLivingBase entity, double factor, float healthAfterLastApply) {
        return apply(entity, factor, NO_FRACTION, healthAfterLastApply, Float.NaN);
    }

    /**
     * Sets the max health factor, then gives the entity back {@code health} (at most its new max health). For an
     * entity the game made with a health value that was clamped before Balance Kit's share existed, such as a player
     * after the End exit.
     */
    public static boolean applyMaxHealthWithHealth(EntityLivingBase entity, double factor, float health) {
        return apply(entity, factor, NO_FRACTION, Float.NaN, health);
    }

    /**
     * Sets the max health factor and keeps the plain health percentage, without the "set to full" exception of
     * {@link #fractionToKeep}: for a change that cannot have been another mod setting a new base, such as a
     * dimension change.
     */
    public static boolean applyMaxHealthKeepingPercentage(EntityLivingBase entity, double factor) {
        return apply(entity, factor, NO_FRACTION, Float.NaN, Float.NaN);
    }

    /**
     * Sets the max health factor right after the entity loaded. The game clamps loaded health to the max health
     * without Balance Kit's modifier, so the fraction saved with the entity is restored instead.
     */
    public static boolean restoreMaxHealth(EntityLivingBase entity, double factor, float savedFraction) {
        return apply(entity, factor, savedFraction, Float.NaN, Float.NaN);
    }

    /**
     * Sets the movement speed factor; an unchanged share costs nothing. Returns false when the entity has no movement
     * speed attribute.
     */
    public static boolean applyMovement(EntityLivingBase entity, double factor) {
        IAttributeInstance instance = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (instance == null) {
            return false;
        }
        setFactor(instance, MOVEMENT_ID, MOVEMENT_NAME, factor);
        return true;
    }

    /** The attribute's current value, or NaN when the entity lacks it. */
    public static double value(EntityLivingBase entity, IAttribute attribute) {
        IAttributeInstance instance = entity.getEntityAttribute(attribute);
        return instance == null ? Double.NaN : instance.getAttributeValue();
    }

    /** The fraction to save with the entity, matching the health that is saved at the same moment. */
    public static float healthFractionForSave(EntityLivingBase entity) {
        float max = entity.getMaxHealth();
        return max > 0.0F ? entity.getHealth() / max : NO_FRACTION;
    }

    /** The attribute's base value, or NaN when the entity lacks it. */
    public static double base(EntityLivingBase entity, IAttribute attribute) {
        IAttributeInstance instance = entity.getEntityAttribute(attribute);
        return instance == null ? Double.NaN : instance.getBaseValue();
    }

    /**
     * The base max health is computed from right now: the underlying base plus Balance Kit's applied share. It can
     * differ from underlying base x configured factor until the next recheck after another mod changed the base.
     * NaN when the entity has no max health attribute.
     */
    public static double appliedMaxHealthBase(EntityLivingBase entity) {
        IAttributeInstance instance = entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        return instance == null ? Double.NaN : appliedBase(instance, MAX_HEALTH_ID);
    }

    static double appliedBase(IAttributeInstance instance, UUID id) {
        AttributeModifier modifier = instance.getModifier(id);
        return instance.getBaseValue() + (modifier == null ? 0.0D : modifier.getAmount());
    }

    /** The highest value the attribute allows (the game's or another mod's cap), or NaN when the entity lacks it. */
    public static double cap(EntityLivingBase entity, IAttribute attribute) {
        IAttributeInstance instance = entity.getEntityAttribute(attribute);
        return instance == null ? Double.NaN : instance.getAttribute().clampValue(Double.MAX_VALUE);
    }

    /**
     * Returns false when the entity has no max health attribute. A dying entity is left alone. Max health is always
     * set before health. {@code exactHealth} (NaN for none) wins over any fraction.
     */
    private static boolean apply(EntityLivingBase entity, double factor, float fixedFraction,
                                 float healthAfterLastApply, float exactHealth) {
        IAttributeInstance instance = entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (instance == null) {
            return false;
        }
        float health = entity.getHealth();
        if (health <= 0.0F) {
            return true;
        }
        float maxWithOldShare = entity.getMaxHealth();
        AttributeModifier existing = instance.getModifier(MAX_HEALTH_ID);
        if (existing != null) {
            instance.removeModifier(existing);
        }
        float maxWithoutShare = entity.getMaxHealth();
        float fraction = isUsableFraction(fixedFraction)
                ? fixedFraction
                : fractionToKeep(health, maxWithOldShare, maxWithoutShare, healthAfterLastApply);
        setFactor(instance, MAX_HEALTH_ID, MAX_HEALTH_NAME, factor);
        float max = entity.getMaxHealth();
        float target = targetHealth(exactHealth, fraction, max, health);
        if (Math.abs(target - health) > 1.0E-4F) {
            entity.setHealth(target);
        }
        return true;
    }

    /**
     * The health fraction to keep when max health is recomputed.
     *
     * <p>Normally that is health over the max health the entity had until now. One exception: when other code raised
     * health since the previous check (at most one recheck interval ago), to exactly the max health without Balance Kit's share (vanilla
     * taming sets a wolf to 20 after raising its base to 20), that code meant "full health" in its own units, so the
     * entity stays full. Requiring the rise keeps an injured entity whose health merely equals that value from being
     * healed. Accepted limit: natural regeneration that lands on exactly that value within the same 20 ticks as a base
     * change also counts as "set to full"; vanilla offers no event that tells the two apart without a Mixin.
     */
    static float fractionToKeep(float health, float maxWithOldShare, float maxWithoutShare, float healthAfterLastApply) {
        if (maxWithOldShare <= 0.0F) {
            return 1.0F;
        }
        boolean raisedSinceLastApply = !Float.isNaN(healthAfterLastApply)
                && health > healthAfterLastApply + SAME_HEALTH;
        if (raisedSinceLastApply && maxWithoutShare > 0.0F && health < maxWithOldShare
                && Math.abs(health - maxWithoutShare) < SAME_HEALTH) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, health / maxWithOldShare));
    }

    /**
     * The health to set once max health is {@code max}: {@code exactHealth} when given (at most {@code max}),
     * otherwise {@code fraction} of {@code max}; an unusable result keeps the current health within the new max.
     */
    static float targetHealth(float exactHealth, float fraction, float max, float health) {
        float target = exactHealth > 0.0F ? Math.min(exactHealth, max) : fraction * max;
        return target <= 0.0F || target > max ? Math.min(health, max) : target;
    }

    static boolean isUsableFraction(float fraction) {
        return fraction > 0.0F && fraction <= 1.0F;
    }

    /**
     * Puts Balance Kit's modifier for {@code factor} on the instance, replacing an older one. A factor of 1 removes it.
     * Returns true when the attribute changed.
     */
    static boolean setFactor(IAttributeInstance instance, UUID id, String name, double factor) {
        double amount = instance.getBaseValue() * (factor - 1.0D);
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null) {
            if (existing.getOperation() == OPERATION_ADD && existing.getAmount() == amount && factor != 1.0D) {
                return false;
            }
            instance.removeModifier(existing);
        }
        if (factor != 1.0D) {
            instance.applyModifier(new AttributeModifier(id, name, amount, OPERATION_ADD).setSaved(false));
            return true;
        }
        return existing != null;
    }
}
