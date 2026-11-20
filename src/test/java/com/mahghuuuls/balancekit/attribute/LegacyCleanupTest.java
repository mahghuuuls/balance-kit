package com.mahghuuuls.balancekit.attribute;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Uses the game's own attribute classes, with modifiers as Scaling Health 1.3.42 writes them (seen in its saves). */
class LegacyCleanupTest {

    private static final UUID EFFECT = UUID.fromString("648d7064-6a60-4f59-8abe-c2c23a6dd7a9");
    private static final UUID OTHER_HEALTH = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final LegacyCleanup ACTIVE = LegacyCleanup.create(true, false);

    /** Max health 20 + Scaling Health 10 (30), attack damage 3 + Scaling Health 10 + a potion's 6. */
    private static AttributeMap scalingHealthMob() {
        AttributeMap map = new AttributeMap();
        map.registerAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
        map.registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
        map.getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH).applyModifier(
                new AttributeModifier(LegacyCleanup.SCALING_HEALTH_HEALTH_ID, "ScalingHealth.HealthModifier", 0.5D, 1));
        map.getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE).applyModifier(
                new AttributeModifier(EFFECT, "effect.damageBoost 1", 6.0D, 0));
        map.getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE).applyModifier(
                new AttributeModifier(LegacyCleanup.SCALING_HEALTH_DAMAGE_ID, "ScalingHealth.DamageModifier", 10.0D, 0));
        return map;
    }

    @Test
    void theIdsAreScalingHealths() {
        assertEquals("c0bef565-35f6-4dc5-bb4c-3644c382e6ce", LegacyCleanup.SCALING_HEALTH_HEALTH_ID.toString());
        assertEquals("d3560b15-c459-451c-86a8-0247015ae899", LegacyCleanup.SCALING_HEALTH_DAMAGE_ID.toString());
    }

    @Test
    void onlyTheTwoScalingHealthModifiersGo() {
        AttributeMap map = scalingHealthMob();
        map.getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH).applyModifier(
                new AttributeModifier(OTHER_HEALTH, "other", 4.0D, 0));

        LegacyCleanup.Result result = ACTIVE.removeFrom(map, 10.0F);

        assertTrue(result.removedAny);
        assertNull(map.getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH)
                .getModifier(LegacyCleanup.SCALING_HEALTH_HEALTH_ID));
        assertNull(map.getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE)
                .getModifier(LegacyCleanup.SCALING_HEALTH_DAMAGE_ID));
        assertNotNull(map.getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH).getModifier(OTHER_HEALTH));
        assertNotNull(map.getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE).getModifier(EFFECT));
        assertEquals(24.0D, map.getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH).getAttributeValue(), 1.0E-9);
        assertEquals(9.0D, map.getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue(), 1.0E-9);
    }

    @Test
    void theFractionIsTakenAgainstTheMaxHealthBeforeTheRemoval() {
        // 24 of 30 before the removal: 0.8. Against the max after the removal (20) it would be 1.2.
        assertEquals(0.8F, ACTIVE.removeFrom(scalingHealthMob(), 24.0F).fractionBefore, 1.0E-6F);
    }

    @Test
    void withoutAHealthModifierThereIsNoFraction() {
        AttributeMap map = scalingHealthMob();
        map.getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH).removeModifier(LegacyCleanup.SCALING_HEALTH_HEALTH_ID);
        LegacyCleanup.Result result = ACTIVE.removeFrom(map, 15.0F);
        assertTrue(result.removedAny, "the damage modifier still goes");
        assertEquals(Contribution.NO_FRACTION, result.fractionBefore);
    }

    @Test
    void anEntityWithoutAttackDamageLosesOnlyTheHealthModifier() {
        AttributeMap map = new AttributeMap();
        map.registerAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
        map.getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH).applyModifier(
                new AttributeModifier(LegacyCleanup.SCALING_HEALTH_HEALTH_ID, "ScalingHealth.HealthModifier", 10.0D, 0));
        LegacyCleanup.Result result = ACTIVE.removeFrom(map, 15.0F);
        assertTrue(result.removedAny);
        assertEquals(0.5F, result.fractionBefore, 1.0E-6F);
        assertEquals(20.0D, map.getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH).getAttributeValue(), 1.0E-9);
    }

    @Test
    void anEntityWithoutMaxHealthDoesNotFail() {
        AttributeMap map = new AttributeMap();
        map.registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
        LegacyCleanup.Result result = ACTIVE.removeFrom(map, 5.0F);
        assertFalse(result.removedAny);
        assertEquals(Contribution.NO_FRACTION, result.fractionBefore);
    }

    @Test
    void offOrWithScalingHealthInstalledNothingIsRemoved() {
        for (LegacyCleanup cleanup : new LegacyCleanup[] {LegacyCleanup.OFF, LegacyCleanup.create(false, false),
                LegacyCleanup.create(false, true), LegacyCleanup.create(true, true)}) {
            AttributeMap map = scalingHealthMob();
            LegacyCleanup.Result result = cleanup.removeFrom(map, 24.0F);
            assertFalse(result.removedAny);
            assertEquals(30.0D, map.getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH).getAttributeValue(), 1.0E-9);
            assertEquals(19.0D, map.getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue(), 1.0E-9);
        }
    }
}
