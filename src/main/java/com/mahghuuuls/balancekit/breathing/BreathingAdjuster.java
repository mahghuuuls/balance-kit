package com.mahghuuuls.balancekit.breathing;

import com.mahghuuuls.balancekit.config.BalanceSettings;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the breathing duration: it never decides when a player loses air, it only scales the loss the game (or another
 * mod) already applied underwater. Air is read at the start and the end of the player's tick; when the player is
 * underwater at the end and air fell while it was above 0 at the tick start, {@link AirLossScaler#adjustedAir}
 * replaces the loss by the scaled loss. Air is never raised above its value at the tick start, never set out of
 * water, and never touched when it was 0 or less at the tick start (it can be set to 0), so Respiration, Water
 * Breathing, recovery and the drowning countdown stay as the game makes them.
 *
 * <p>On the server this runs for every real player, with the configured k. The server also sends k to each client
 * that has Balance Kit ({@link BreathingMessage}); such a client runs the same scaling for its own player only, so its
 * prediction of the air matches the server and the bubbles do not jump. A client that got no value does nothing.
 *
 * <p>One scaler per player UUID on each side, so a respawn keeps the balance; it is dropped at logout. Fake players are
 * not players for Balance Kit and get nothing.
 */
public final class BreathingAdjuster {

    private static final Logger LOG = LogManager.getLogger("Balance Kit");
    private static final int NO_READING = Integer.MIN_VALUE;
    private final double multiplier;
    @Nullable
    private final SimpleNetworkWrapper channel;
    private final String modId;
    private final Map<UUID, Track> serverTracks = new HashMap<>();
    private final Map<UUID, Track> clientTracks = new HashMap<>();
    /** The server's k as received by this client; NaN until a message arrives and after leaving a server. */
    private volatile double clientMultiplier = Double.NaN;
    private boolean reported;
    /** Whether this client has logged its first adjustment with the server's value since it joined. */
    private boolean clientAdjustmentLogged;

    /**
     * {@code channel} sends k to clients whose mod list holds {@code modId}; a null channel leaves clients without it
     * (tests).
     */
    public BreathingAdjuster(BalanceSettings settings, @Nullable SimpleNetworkWrapper channel, String modId) {
        this.multiplier = settings.playerBreathing();
        this.channel = channel;
        this.modId = modId;
    }

    /** The breathing multiplier k, as the report shows it. */
    public double multiplier() {
        return multiplier;
    }

    /** Called on the client's game thread when the server's k arrives. */
    public void receiveServerMultiplier(double serverMultiplier) {
        clientMultiplier = serverMultiplier > 0.0D ? serverMultiplier : Double.NaN;
        clientAdjustmentLogged = false;
        LOG.debug("Breathing multiplier from the server: {} (this client's own setting, not used: {})", serverMultiplier,
                multiplier);
    }

    /**
     * The k a side uses: the server its configured value, a client only the value the server sent (NaN, so nothing,
     * before it arrives). The client's own config is never used for its player.
     */
    double effectiveMultiplier(boolean client) {
        return client ? clientMultiplier : multiplier;
    }

    /** The k this client uses for its own player, or NaN when it has none. */
    double clientMultiplier() {
        return clientMultiplier;
    }

    @SubscribeEvent
    public void onLogin(PlayerLoggedInEvent event) {
        send(event.player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerRespawnEvent event) {
        send(event.player);
    }

    @SubscribeEvent
    public void onLogout(PlayerLoggedOutEvent event) {
        serverTracks.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void onLeaveServer(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // On the game thread, after any value still queued from this connection, so it cannot come back.
        FMLCommonHandler.instance().getWorldThread(event.getHandler()).addScheduledTask(() -> {
            clientMultiplier = Double.NaN;
            clientTracks.clear();
        });
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (player instanceof FakePlayer) {
            return;
        }
        boolean client = player.world.isRemote;
        if (client && !player.isUser()) {
            return;
        }
        double k = effectiveMultiplier(client);
        if (Double.isNaN(k) || k == 1.0D) {
            return;
        }
        try {
            Map<UUID, Track> tracks = client ? clientTracks : serverTracks;
            Track track = tracks.get(player.getUniqueID());
            if (track == null || track.multiplier != k) {
                track = new Track(k);
                tracks.put(player.getUniqueID(), track);
            }
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
                if (client && !clientAdjustmentLogged) {
                    clientAdjustmentLogged = true;
                    LOG.debug("Breathing: this client scales its player's air with the server's multiplier {} ({} -> {})",
                            k, after, target);
                }
            }
        } catch (RuntimeException e) {
            if (!reported) {
                reported = true;
                LOG.warn("Breathing could not be adjusted for {}; air left as the game set it", player.getName(), e);
            }
        }
    }

    /** Sends k to a real player's client when that client has Balance Kit; a client without it gets nothing. */
    private void send(EntityPlayer player) {
        if (channel == null || !(player instanceof EntityPlayerMP) || player instanceof FakePlayer) {
            return;
        }
        try {
            EntityPlayerMP target = (EntityPlayerMP) player;
            NetworkDispatcher dispatcher = NetworkDispatcher.get(target.connection.getNetworkManager());
            if (dispatcher != null && dispatcher.getModList().containsKey(modId)) {
                channel.sendTo(new BreathingMessage(multiplier), target);
            }
        } catch (RuntimeException e) {
            LOG.warn("Could not send the breathing multiplier to {}; its client predicts vanilla air loss",
                    player.getName(), e);
        }
    }

    private static final class Track {
        final double multiplier;
        final AirLossScaler scaler;
        int airAtStart = NO_READING;

        Track(double multiplier) {
            this.multiplier = multiplier;
            this.scaler = new AirLossScaler(multiplier);
        }
    }
}
