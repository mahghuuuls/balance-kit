package com.mahghuuuls.balancekit.attribute;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Uses the game's own attribute classes, so the stacking order is the game's, not a copy of it. */
class ContributionTest {

    private static final UUID OTHER_MOD = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private static IAttributeInstance maxHealth(double base) {
        IAttributeInstance instance = new AttributeMap().registerAttribute(SharedMonsterAttributes.MAX_HEALTH);
        instance.setBaseValue(base);
        return instance;
    }

    private static boolean apply(IAttributeInstance instance, double factor) {
        return Contribution.setFactor(instance, Contribution.MAX_HEALTH_ID, Contribution.MAX_HEALTH_NAME, factor);
    }

    @Test
    void aSavedFractionWinsOverTheFallback() {
        assertEquals(0.5F, Contribution.preferredFraction(0.5F, 0.8F), 0.0F);
        assertEquals(0.8F, Contribution.preferredFraction(Contribution.NO_FRACTION, 0.8F), 0.0F);
        assertEquals(0.8F, Contribution.preferredFraction(0.0F, 0.8F), 0.0F);
        assertEquals(Contribution.NO_FRACTION, Contribution.preferredFraction(Contribution.NO_FRACTION,
                Contribution.NO_FRACTION), 0.0F);
    }

    @Test
    void factorActsLikeAChangedBase() {
        IAttributeInstance instance = maxHealth(20.0D);
        assertTrue(apply(instance, 2.0D));
        assertEquals(40.0D, instance.getAttributeValue(), 1.0E-9);
        assertEquals(20.0D, instance.getBaseValue(), 1.0E-9);
    }

    @Test
    void otherModsAddModifierIsNotMultiplied() {
        IAttributeInstance instance = maxHealth(20.0D);
        instance.applyModifier(new AttributeModifier(OTHER_MOD, "other", 4.0D, 0));
        apply(instance, 2.0D);
        assertEquals(44.0D, instance.getAttributeValue(), 1.0E-9);
    }

    @Test
    void aPlayerBaseOf30WithAnotherModsPlusFourGives34() {
        IAttributeInstance instance = maxHealth(20.0D);
        assertTrue(apply(instance, 30.0D / 20.0D));
        assertEquals(30.0D, instance.getAttributeValue(), 1.0E-9);
        instance.applyModifier(new AttributeModifier(OTHER_MOD, "other", 4.0D, 0));
        assertEquals(34.0D, instance.getAttributeValue(), 1.0E-9);
        assertTrue(apply(instance, 1.0D));
        assertEquals(24.0D, instance.getAttributeValue(), 1.0E-9, "switch off: the game's base again");
    }

    @Test
    void anExactHealthWinsOverTheFractionAndStaysWithinTheNewMax() {
        assertEquals(28.0F, Contribution.targetHealth(28.0F, 0.5F, 30.0F, 20.0F), 1.0E-6F);
        assertEquals(30.0F, Contribution.targetHealth(40.0F, 0.5F, 30.0F, 20.0F), 1.0E-6F);
        assertEquals(15.0F, Contribution.targetHealth(Float.NaN, 0.5F, 30.0F, 20.0F), 1.0E-6F);
        assertEquals(25.0F, Contribution.targetHealth(Float.NaN, 0.0F, 30.0F, 25.0F), 1.0E-6F);
        assertEquals(20.0F, Contribution.targetHealth(Float.NaN, 0.0F, 20.0F, 25.0F), 1.0E-6F);
    }

    @Test
    void movementFactorThenASpeedEffectMultipliesTheTotal() {
        AttributeMap map = new AttributeMap();
        IAttributeInstance instance = map.registerAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        instance.setBaseValue(0.1D);
        assertTrue(Contribution.setFactor(instance, Contribution.MOVEMENT_ID, Contribution.MOVEMENT_NAME, 1.2D));
        assertEquals(0.12D, instance.getAttributeValue(), 1.0E-9);
        instance.applyModifier(new AttributeModifier(OTHER_MOD, "speed II", 0.4D, 2));
        assertEquals(0.168D, instance.getAttributeValue(), 1.0E-9);
        assertFalse(Contribution.MOVEMENT_ID.equals(Contribution.MAX_HEALTH_ID), "its own modifier id");
    }

    @Test
    void attackSpeedMultipliesTheFinalValueAfterTheWeapon() {
        AttributeMap map = new AttributeMap();
        IAttributeInstance instance = map.registerAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        instance.setBaseValue(4.0D);
        instance.applyModifier(new AttributeModifier(OTHER_MOD, "diamond sword", -2.4D, 0));
        assertEquals(1.6D, instance.getAttributeValue(), 1.0E-9);
        assertTrue(Contribution.setTotalFactor(instance, Contribution.ATTACK_SPEED_ID, Contribution.ATTACK_SPEED_NAME, 2.0D));
        assertEquals(3.2D, instance.getAttributeValue(), 1.0E-9);
        // Another mod's multiply-base +50 percent: a multiply-total share doubles 1.6 x 1.5 = 2.4 to 4.8; a
        // multiply-base share would add to that +50 percent instead and give 1.6 x 2.5 = 4.0.
        UUID haste = UUID.fromString("22222222-3333-4444-5555-666666666666");
        instance.applyModifier(new AttributeModifier(haste, "haste", 0.5D, 1));
        assertEquals(4.8D, instance.getAttributeValue(), 1.0E-9);
        instance.removeModifier(haste);
        assertFalse(Contribution.setTotalFactor(instance, Contribution.ATTACK_SPEED_ID, Contribution.ATTACK_SPEED_NAME, 2.0D),
                "reapplying does not stack");
        assertEquals(3.2D, instance.getAttributeValue(), 1.0E-9);
        assertFalse(instance.getModifier(Contribution.ATTACK_SPEED_ID).isSaved());
        assertTrue(Contribution.setTotalFactor(instance, Contribution.ATTACK_SPEED_ID, Contribution.ATTACK_SPEED_NAME, 1.0D));
        assertEquals(1.6D, instance.getAttributeValue(), 1.0E-9);
    }

    @Test
    void otherModsMultiplyBaseModifierSeesTheAdjustedBase() {
        IAttributeInstance instance = maxHealth(20.0D);
        instance.applyModifier(new AttributeModifier(OTHER_MOD, "other", 0.5D, 1));
        apply(instance, 2.0D);
        assertEquals(60.0D, instance.getAttributeValue(), 1.0E-9);
    }

    @Test
    void modifierIsNeverSaved() {
        IAttributeInstance instance = maxHealth(20.0D);
        apply(instance, 2.0D);
        assertFalse(instance.getModifier(Contribution.MAX_HEALTH_ID).isSaved());
    }

    @Test
    void reapplyingDoesNotStack() {
        IAttributeInstance instance = maxHealth(20.0D);
        apply(instance, 2.0D);
        assertFalse(apply(instance, 2.0D));
        apply(instance, 2.0D);
        assertEquals(40.0D, instance.getAttributeValue(), 1.0E-9);
    }

    @Test
    void newBaseIsMultipliedAfresh() {
        IAttributeInstance instance = maxHealth(16.0D);
        apply(instance, 2.0D);
        instance.setBaseValue(16.0D);
        apply(instance, 2.0D);
        assertEquals(32.0D, instance.getAttributeValue(), 1.0E-9);
        instance.setBaseValue(9.0D);
        assertTrue(apply(instance, 2.0D));
        assertEquals(18.0D, instance.getAttributeValue(), 1.0E-9);
    }

    @Test
    void neutralFactorRemovesTheModifier() {
        IAttributeInstance instance = maxHealth(20.0D);
        apply(instance, 2.0D);
        assertTrue(apply(instance, 1.0D));
        assertNull(instance.getModifier(Contribution.MAX_HEALTH_ID));
        assertEquals(20.0D, instance.getAttributeValue(), 1.0E-9);
        assertFalse(apply(instance, 1.0D));
    }

    @Test
    void appliedBaseIsWhatTheGameUsesEvenAfterTheBaseChangedUnderIt() {
        IAttributeInstance instance = maxHealth(8.0D);
        apply(instance, 2.0D);
        assertEquals(16.0D, Contribution.appliedBase(instance, Contribution.MAX_HEALTH_ID), 1.0E-9);
        // Vanilla taming raises the base to 20; Balance Kit's +8 is still on until the next recheck.
        instance.setBaseValue(20.0D);
        assertEquals(28.0D, Contribution.appliedBase(instance, Contribution.MAX_HEALTH_ID), 1.0E-9);
        assertEquals(28.0D, instance.getAttributeValue(), 1.0E-9);
        apply(instance, 2.0D);
        assertEquals(40.0D, Contribution.appliedBase(instance, Contribution.MAX_HEALTH_ID), 1.0E-9);
        assertEquals(20.0D, Contribution.appliedBase(maxHealth(20.0D), Contribution.MAX_HEALTH_ID), 1.0E-9);
    }

    @Test
    void keepsTheFractionOfTheMaxHealthTheEntityHad() {
        assertEquals(0.5F, Contribution.fractionToKeep(10.0F, 20.0F, 20.0F, 10.0F), 1.0E-6F);
        assertEquals(0.75F, Contribution.fractionToKeep(30.0F, 40.0F, 20.0F, 30.0F), 1.0E-6F);
    }

    @Test
    void healthSetToTheUnscaledMaxByOtherCodeStaysFull() {
        // Vanilla taming: full wild wolf at 16, base 8 -> 20 with the stale +8 still on (max 28), then setHealth(20).
        assertEquals(1.0F, Contribution.fractionToKeep(20.0F, 28.0F, 20.0F, 16.0F), 1.0E-6F);
    }

    @Test
    void aGrowingBaseWithoutAHealthChangeKeepsTheVisibleFraction() {
        // Base 100 -> 120 under factor 2: stale max 220, health 150 untouched by other code.
        assertEquals(150.0F / 220.0F, Contribution.fractionToKeep(150.0F, 220.0F, 120.0F, 150.0F), 1.0E-6F);
    }

    @Test
    void anInjuredEntityWhoseHealthEqualsTheUnscaledMaxIsNotHealed() {
        // Base 100 -> 120 under factor 2 while at 120 of 220, health unchanged since the last adjustment.
        assertEquals(120.0F / 220.0F, Contribution.fractionToKeep(120.0F, 220.0F, 120.0F, 120.0F), 1.0E-6F);
        // Without a known earlier health the exception never applies.
        assertEquals(20.0F / 28.0F, Contribution.fractionToKeep(20.0F, 28.0F, 20.0F, Float.NaN), 1.0E-6F);
    }

    @Test
    void savedFractionsOutsideTheRangeAreNotUsed() {
        assertFalse(Contribution.isUsableFraction(Contribution.NO_FRACTION));
        assertFalse(Contribution.isUsableFraction(0.0F));
        assertFalse(Contribution.isUsableFraction(1.5F));
        assertTrue(Contribution.isUsableFraction(1.0F));
    }
}
