package com.mahghuuuls.balancekit.damage;

import com.mahghuuuls.balancekit.config.ConfigWarning;
import com.mahghuuuls.balancekit.config.SettingsParser;
import com.mahghuuuls.balancekit.mob.MobBalancer;
import com.mahghuuuls.balancekit.rules.RuleBook;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.FakePlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The formula, the exceptions and who counts as a player, through the real settings parser. Entities are made
 * without running their constructors (which need a world), so only their type is real; mob profiles are runtime
 * checks.
 */
class DamageScalerTest {

    private static DamageScaler scaler(Map<String, String> raw, String... excluded) {
        Map<String, String[]> lists = new HashMap<>();
        lists.put(SettingsParser.EXCLUDED_DAMAGE_TYPES, excluded);
        SettingsParser.Result result = SettingsParser.parse(raw, lists);
        for (ConfigWarning warning : result.warnings()) {
            throw new AssertionError(warning.toString());
        }
        return new DamageScaler(result.settings(), new MobBalancer(new RuleBook(result.settings())));
    }

    private static Map<String, String> players(String dealt, String received) {
        Map<String, String> raw = new HashMap<>();
        raw.put(SettingsParser.PLAYER_DEALT, dealt);
        raw.put(SettingsParser.PLAYER_RECEIVED, received);
        return raw;
    }

    /** An instance whose constructor never ran, through the JDK's allocator, reached by reflection. */
    private static <T extends Entity> T bare(Class<T> type) {
        try {
            Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
            Field field = unsafeType.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Method allocate = unsafeType.getMethod("allocateInstance", Class.class);
            return type.cast(allocate.invoke(field.get(null), type));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void theAmountIsMultipliedByBothSides() {
        assertEquals(18.0F, DamageScaler.scale(6.0F, 2.0D, 1.5D), 1.0E-6);
        assertEquals(12.0F, DamageScaler.scale(4.0F, 2.0D, 1.5D), 1.0E-6);
        assertEquals(0.0F, DamageScaler.scale(6.0F, 2.0D, 0.0D), 0.0F);
    }

    @Test
    void zeroStaysZeroForAnInfiniteAmount() {
        assertEquals(0.0F, DamageScaler.scale(Float.POSITIVE_INFINITY, 0.0D, 1.5D), 0.0F);
        assertEquals(0.0F, DamageScaler.scale(Float.POSITIVE_INFINITY, 2.0D, 0.0D), 0.0F);
        assertEquals(Float.POSITIVE_INFINITY, DamageScaler.scale(Float.POSITIVE_INFINITY, 2.0D, 1.5D), 0.0F);
    }

    @Test
    void neutralSettingsLeaveTheHandlerInactive() {
        assertFalse(scaler(new HashMap<>()).active());
        assertTrue(scaler(players("2", "1")).active());
        Map<String, String> mobs = new HashMap<>();
        mobs.put(SettingsParser.MOBS_RECEIVED, "0");
        assertTrue(scaler(mobs).active());
        Map<String, String[]> lists = new HashMap<>();
        lists.put(SettingsParser.ENTITY_RULES, new String[] {"minecraft:zombie dealt=1.5"});
        SettingsParser.Result result = SettingsParser.parse(new HashMap<>(), lists);
        assertTrue(new DamageScaler(result.settings(), new MobBalancer(new RuleBook(result.settings()))).active());
        lists.put(SettingsParser.ENTITY_RULES, new String[] {"minecraft:zombie health=2 dealt=1"});
        result = SettingsParser.parse(new HashMap<>(), lists);
        assertFalse(new DamageScaler(result.settings(), new MobBalancer(new RuleBook(result.settings()))).active());
    }

    @Test
    void outOfWorldAndExcludedTypesAreNeverScaled() {
        DamageScaler scaler = scaler(players("2", "0"), "fall", " lava ");
        assertEquals(DamageScaler.Skip.OUT_OF_WORLD, scaler.skipReason("outOfWorld"));
        assertEquals(DamageScaler.Skip.EXCLUDED, scaler.skipReason("fall"));
        assertEquals(DamageScaler.Skip.EXCLUDED, scaler.skipReason("lava"));
        assertNull(scaler.skipReason("player"));
        assertNull(scaler.skipReason("Fall"), "names are exact");
        assertEquals(DamageScaler.Skip.OUT_OF_WORLD, scaler(new HashMap<>()).skipReason("outOfWorld"));
    }

    @Test
    void playersGetThePlayerSettingsAndFakePlayersAndOthersGetNothing() {
        DamageScaler scaler = scaler(players("3", "1.5"));
        EntityPlayerMP player = bare(EntityPlayerMP.class);
        FakePlayer fake = bare(FakePlayer.class);
        EntityArmorStand stand = bare(EntityArmorStand.class);
        assertEquals(3.0D, scaler.dealtBy(player), 1.0E-9);
        assertEquals(1.5D, scaler.receivedBy(player), 1.0E-9);
        assertEquals(1.0D, scaler.dealtBy(fake), 1.0E-9);
        assertEquals(1.0D, scaler.receivedBy(fake), 1.0E-9);
        assertEquals(1.0D, scaler.dealtBy(stand), 1.0E-9);
        assertEquals(1.0D, scaler.receivedBy(stand), 1.0E-9);
        assertEquals(1.0D, scaler.dealtBy(null), 1.0E-9, "damage naming nobody");
        assertTrue(DamageScaler.isPlayer(player));
        assertFalse(DamageScaler.isPlayer(fake));
    }

    @Test
    void aFakePlayersHitGetsOnlyTheVictimsMultiplier() {
        DamageScaler scaler = scaler(players("3", "1.5"));
        EntityPlayerMP victim = bare(EntityPlayerMP.class);
        float amount = DamageScaler.scale(4.0F, scaler.dealtBy(bare(FakePlayer.class)), scaler.receivedBy(victim));
        assertEquals(6.0F, amount, 1.0E-6);
    }

    @Test
    void theLogLineNamesTypeParticipantsAndDecision() {
        assertEquals("Damage: type=player attacker=player Dev victim=minecraft:zombie#7 dealt x2 received x1.5 amount 6 -> 18",
                DamageScaler.logLine("player", "player Dev", "minecraft:zombie#7", null, 2.0D, 1.5D, 6.0F, 18.0F));
        assertEquals("Damage: type=fall attacker=none victim=player Dev not scaled (excluded type) amount 3 -> 3",
                DamageScaler.logLine("fall", "none", "player Dev", "excluded type", 1.0D, 1.0D, 3.0F, 3.0F));
    }
}
