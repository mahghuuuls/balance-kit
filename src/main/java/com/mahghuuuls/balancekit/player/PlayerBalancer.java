package com.mahghuuuls.balancekit.player;

import com.mahghuuuls.balancekit.attribute.Contribution;
import com.mahghuuuls.balancekit.config.BalanceSettings;
import com.mahghuuuls.balancekit.state.BalanceState;
import com.mahghuuuls.balancekit.state.BalanceStates;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Owns the player lifecycle: every path that makes or reloads a player entity ends in {@link #apply}, which sets the
 * player's max health from the configured base (or removes Balance Kit's share when the switch is off).
 *
 * <ul>
 *   <li>Login: the game loads health clamped to the max health without Balance Kit's share, so the fraction saved
 *   with the player is restored.</li>
 *   <li>Death and the End exit replace the player entity: the saved state is copied in {@code Clone}, and after the
 *   End exit the health the old entity had is given back, since the game copied it before the share existed.</li>
 *   <li>Dimension travel keeps the entity; its share is checked again.</li>
 *   <li>Every 20 ticks the base is compared with the one last used, so a base changed by the game or another mod
 *   gets a fresh share.</li>
 * </ul>
 *
 * <p>Every hook goes through one private {@code apply}. Runs on the logical server thread only. Fake players are not
 * players for Balance Kit and get nothing.
 */
public final class PlayerBalancer {

    static final int RECHECK_INTERVAL = 20;

    private static final Logger LOG = LogManager.getLogger("Balance Kit");

    private final BalanceSettings settings;
    private final Set<String> reportedFailures = new HashSet<>();

    public PlayerBalancer(BalanceSettings settings) {
        this.settings = settings;
    }

    /** A real player: fake players (automation stand-ins) are neither players nor mobs for Balance Kit. */
    public static boolean isPlayer(Entity entity) {
        return entity instanceof EntityPlayer && !(entity instanceof FakePlayer);
    }

    @SubscribeEvent
    public void onAttach(AttachCapabilitiesEvent<Entity> event) {
        BalanceStates.attach(event, PlayerBalancer::isPlayer, Contribution::healthFractionForSave);
    }

    @SubscribeEvent
    public void onLogin(PlayerLoggedInEvent event) {
        BalanceState state = serverState(event.player);
        if (state != null) {
            float saved = state.takeSavedHealthFraction();
            apply(event.player, state, factor -> Contribution.restoreMaxHealth(event.player, factor, saved));
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        BalanceState old = BalanceStates.get(event.getOriginal());
        BalanceState now = serverState(event.getEntityPlayer());
        if (old == null || now == null) {
            return;
        }
        now.copySavedFrom(old);
        if (!event.isWasDeath()) {
            now.setHealthToRestore(event.getOriginal().getHealth());
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerRespawnEvent event) {
        BalanceState state = serverState(event.player);
        if (state != null) {
            float health = state.takeHealthToRestore();
            apply(event.player, state, factor -> health > 0.0F
                    ? Contribution.applyMaxHealthWithHealth(event.player, factor, health)
                    : Contribution.applyMaxHealthKeepingPercentage(event.player, factor));
        }
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerChangedDimensionEvent event) {
        BalanceState state = serverState(event.player);
        if (state != null) {
            apply(event.player, state, factor -> Contribution.applyMaxHealthKeepingPercentage(event.player, factor));
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase != TickEvent.Phase.END || (player.ticksExisted + player.getEntityId()) % RECHECK_INTERVAL != 0) {
            return;
        }
        BalanceState state = serverState(player);
        if (state == null) {
            return;
        }
        if (Contribution.base(player, SharedMonsterAttributes.MAX_HEALTH) != state.lastHealthBase()) {
            apply(player, state, factor -> Contribution.applyMaxHealth(player, factor, state.healthAfterLastApply()));
        } else {
            // Keeps "health rose since the last check" to one recheck interval (see Contribution.fractionToKeep).
            state.setHealthAfterLastApply(player.getHealth());
        }
    }

    /** What Balance Kit decided for one player's max health, from the same values this class applies. */
    public PlayerHealthReport report(EntityPlayer player) {
        return new PlayerHealthReport(settings.playerBaseHealthEnabled(), settings.playerBaseHealth(),
                Contribution.base(player, SharedMonsterAttributes.MAX_HEALTH), Contribution.appliedMaxHealthBase(player),
                player.getMaxHealth(), player.getHealth(), Contribution.cap(player, SharedMonsterAttributes.MAX_HEALTH));
    }

    /** The max health factor that makes the base equal the configured value; 1 (no share) while the switch is off. */
    double factor(EntityPlayer player) {
        return factor(settings.playerBaseHealthEnabled(), settings.playerBaseHealth(),
                Contribution.base(player, SharedMonsterAttributes.MAX_HEALTH));
    }

    static double factor(boolean enabled, double configuredBase, double underlyingBase) {
        if (!enabled || !(underlyingBase > 0.0D)) {
            return 1.0D;
        }
        return configuredBase / underlyingBase;
    }

    private static BalanceState serverState(EntityPlayer player) {
        return player.world.isRemote || !isPlayer(player) ? null : BalanceStates.get(player);
    }

    /**
     * The one path every hook takes: computes the factor from the current config, lets {@code step} set it (each
     * hook chooses how health is kept), and remembers the base and health it left for the next recheck.
     */
    private void apply(EntityPlayer player, BalanceState state, Step step) {
        try {
            state.setLastHealthBase(Contribution.base(player, SharedMonsterAttributes.MAX_HEALTH));
            boolean applied = step.apply(factor(player));
            if (!applied) {
                reportOnce(player, "has no max health attribute; left unchanged", null);
            }
            state.setHealthAfterLastApply(player.getHealth());
        } catch (RuntimeException e) {
            reportOnce(player, "could not be balanced; left unchanged", e);
        }
    }

    private void reportOnce(EntityPlayer player, String problem, Throwable cause) {
        String type = player.getClass().getName();
        if (reportedFailures.add(type + problem)) {
            if (cause == null) {
                LOG.warn("Player type {} {}", type, problem);
            } else {
                LOG.warn("Player type {} {}", type, problem, cause);
            }
        }
    }

    private interface Step {
        boolean apply(double factor);
    }
}
