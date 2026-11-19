package com.mahghuuuls.balancekit.damage;

import com.mahghuuuls.balancekit.config.BalanceSettings;
import com.mahghuuuls.balancekit.mob.MobBalancer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Owns damage scaling: amount x attacker's damage-dealt multiplier x victim's damage-received multiplier, applied in
 * {@link LivingHurtEvent}, which fires before armor, enchantment protection, resistance and absorption.
 *
 * <ul>
 *   <li>The attacker is the entity the damage names as responsible ({@link DamageSource#getTrueSource()}): the
 *   hitter, the shooter of a projectile, the igniter of TNT. Nothing else is guessed; damage naming nobody (fall,
 *   fire left burning, poison) gets only the victim's multiplier.</li>
 *   <li>{@code outOfWorld} (the void and /kill) and the configured excluded types are never scaled.</li>
 *   <li>Players get the player settings; mobs get their resolved profile, which already applies the damage
 *   blocklist; fake players and other entities get 1.</li>
 *   <li>Damage the game blocks (PvP off, creative, invulnerability) never reaches this event, so it is never
 *   created here; a result of 0 removes no health but the hit still counts for knockback and anger.</li>
 * </ul>
 *
 * <p>Runs on the logical server thread only.
 */
public final class DamageScaler {

    static final String OUT_OF_WORLD = "outOfWorld";

    /** Why a damage type is never scaled. */
    enum Skip {
        OUT_OF_WORLD("outOfWorld is never scaled"),
        EXCLUDED("excluded type");

        final String reason;

        Skip(String reason) {
            this.reason = reason;
        }
    }

    private static final Logger LOG = LogManager.getLogger("Balance Kit");

    private final BalanceSettings settings;
    private final MobBalancer mobs;
    /** False when every damage multiplier is 1, so a hit needs no lookup at all. */
    private final boolean active;
    private final Set<String> reportedFailures = new HashSet<>();

    public DamageScaler(BalanceSettings settings, MobBalancer mobs) {
        this.settings = settings;
        this.mobs = mobs;
        this.active = settings.playerDealt() != 1.0D || settings.playerReceived() != 1.0D || mobs.changesMobDamage();
    }

    /** False when no player or mob damage multiplier differs from 1, so every hit stays as the game made it. */
    boolean active() {
        return active;
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (!active && !settings.logDamageTypes()) {
            return;
        }
        Entity victim = event.getEntityLiving();
        if (victim.world.isRemote) {
            return;
        }
        DamageSource source = event.getSource();
        float before = event.getAmount();
        Skip skipped;
        Entity attacker;
        double dealt = 1.0D;
        double received = 1.0D;
        try {
            skipped = skipReason(source.getDamageType());
            attacker = source.getTrueSource();
            if (skipped == null && active) {
                dealt = dealtBy(attacker);
                received = receivedBy(victim);
                // Everything that can fail runs before this line, so a failure leaves the hit as the game made it.
                event.setAmount(scale(before, dealt, received));
            }
        } catch (RuntimeException e) {
            reportOnce(victim, "could not be scaled; left unchanged", e);
            return;
        }
        if (settings.logDamageTypes()) {
            // Separate from the scaling, so turning the log on never changes damage.
            try {
                LOG.info(logLine(source.getDamageType(), describe(attacker), describe(victim),
                        skipped == null ? null : skipped.reason, dealt, received, before, event.getAmount()));
            } catch (RuntimeException e) {
                reportOnce(victim, "could not be described in the damage log", e);
            }
        }
    }

    private void reportOnce(Entity victim, String problem, RuntimeException cause) {
        String type = victim.getClass().getName();
        if (reportedFailures.add(type + problem)) {
            LOG.warn("Damage to entity type {} {}", type, problem, cause);
        }
    }

    /** Why damage of this type is never scaled, or null when it is scaled. */
    @Nullable
    Skip skipReason(String damageType) {
        if (OUT_OF_WORLD.equals(damageType)) {
            return Skip.OUT_OF_WORLD;
        }
        if (settings.excludedDamageTypes().contains(damageType)) {
            return Skip.EXCLUDED;
        }
        return null;
    }

    /** The damage-dealt multiplier of the entity a damage names as responsible; 1 when it names nobody. */
    public double dealtBy(@Nullable Entity attacker) {
        if (isPlayer(attacker)) {
            return settings.playerDealt();
        }
        if (MobBalancer.isMob(attacker)) {
            return mobs.profileOf((EntityLiving) attacker).dealt();
        }
        return 1.0D;
    }

    /** The damage-received multiplier of a victim. */
    public double receivedBy(@Nullable Entity victim) {
        if (isPlayer(victim)) {
            return settings.playerReceived();
        }
        if (MobBalancer.isMob(victim)) {
            return mobs.profileOf((EntityLiving) victim).received();
        }
        return 1.0D;
    }

    /** A real player: fake players (automation stand-ins) are neither players nor mobs for Balance Kit. */
    public static boolean isPlayer(@Nullable Entity entity) {
        return entity instanceof EntityPlayer && !(entity instanceof FakePlayer);
    }

    /** A multiplier of 0 gives 0 even for an infinite amount, which would otherwise give NaN health. */
    static float scale(float amount, double dealt, double received) {
        double factor = dealt * received;
        return factor == 0.0D ? 0.0F : (float) (amount * factor);
    }

    static String logLine(String type, String attacker, String victim, @Nullable String skipped, double dealt,
                          double received, float before, float after) {
        String decision = skipped != null ? "not scaled (" + skipped + ")"
                : "dealt x" + number(dealt) + " received x" + number(received);
        return "Damage: type=" + type + " attacker=" + attacker + " victim=" + victim + " " + decision + " amount "
                + number(before) + " -> " + number(after);
    }

    private static String describe(@Nullable Entity entity) {
        if (entity == null) {
            return "none";
        }
        if (entity instanceof EntityPlayer) {
            return (isPlayer(entity) ? "player " : "fake player ") + entity.getName();
        }
        ResourceLocation key = EntityList.getKey(entity);
        return (key == null ? entity.getClass().getName() : key.toString()) + "#" + entity.getEntityId();
    }

    private static String number(double value) {
        String text = String.format(Locale.ROOT, "%.3f", value);
        return text.replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
