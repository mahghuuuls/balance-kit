package com.mahghuuuls.balancekit.breathing;

import com.mahghuuuls.balancekit.config.BalanceSettings;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the breathing duration: it never decides when a player loses air, it only scales the loss the game (or another
 * mod) already applied underwater. Air is read at the start and the end of the player's tick; when the player is
 * underwater at the end and air fell while it was above 0, {@link AirLossScaler#adjustedAir} replaces the loss by the
 * scaled loss. Air is never raised above its value at the tick start, never set out of water and never set at or
 * below 0, so Respiration, Water Breathing, recovery and the drowning countdown stay as the game makes them.
 *
 * <p>One scaler per player, by UUID, so a respawn keeps the player's balance; it is dropped at logout. Runs on the
 * logical server thread only. Fake players are not players for Balance Kit and get nothing.
 */
public final class BreathingAdjuster {

    private static final Logger LOG = LogManager.getLogger("Balance Kit");
    private static final int NO_READING = Integer.MIN_VALUE;

    private final double multiplier;
    private final Map<UUID, Track> tracks = new HashMap<>();
    private boolean reported;

    public BreathingAdjuster(BalanceSettings settings) {
        this.multiplier = settings.playerBreathing();
    }

    /** The breathing multiplier k, as the report shows it. */
    public double multiplier() {
        return multiplier;
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (multiplier == 1.0D || player.world.isRemote || player instanceof FakePlayer) {
            return;
        }
        try {
            Track track = tracks.computeIfAbsent(player.getUniqueID(), id -> new Track(multiplier));
            if (event.phase == TickEvent.Phase.START) {
                track.airAtStart = player.getAir();
                return;
            }
            int before = track.airAtStart;
            track.airAtStart = NO_READING;
            if (before == NO_READING || !player.isInsideOfMaterial(Material.WATER)) {
                return;
            }
            int after = player.getAir();
            int target = track.scaler.adjustedAir(before, after);
            if (target != after) {
                player.setAir(target);
            }
        } catch (RuntimeException e) {
            if (!reported) {
                reported = true;
                LOG.warn("Breathing could not be adjusted for {}; air left as the game set it", player.getName(), e);
            }
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerLoggedOutEvent event) {
        tracks.remove(event.player.getUniqueID());
    }

    private static final class Track {
        final AirLossScaler scaler;
        int airAtStart = NO_READING;

        Track(double multiplier) {
            this.scaler = new AirLossScaler(multiplier);
        }
    }
}
