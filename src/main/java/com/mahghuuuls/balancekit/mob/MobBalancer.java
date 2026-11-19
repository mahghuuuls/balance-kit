package com.mahghuuuls.balancekit.mob;

import com.mahghuuuls.balancekit.attribute.Contribution;
import com.mahghuuuls.balancekit.config.BalanceSettings;
import com.mahghuuuls.balancekit.state.BalanceState;
import com.mahghuuuls.balancekit.state.BalanceStates;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Owns when a mob is balanced.
 *
 * <ul>
 *   <li>A new mob is marked at its world join and balanced on its first server tick, because some spawns (animals
 *   made with new chunks) only get their variant after joining the world.</li>
 *   <li>A mob that was already balanced is rebuilt at its join, from the current config, and its saved health
 *   fraction is restored.</li>
 *   <li>Every 20 ticks the base is compared with the one last used, so a base changed by the game or another mod is
 *   multiplied afresh instead of keeping a stale bonus.</li>
 * </ul>
 *
 * <p>Runs on the logical server thread only.
 */
public final class MobBalancer {

    static final int RECHECK_INTERVAL = 20;

    private static final Logger LOG = LogManager.getLogger("Balance Kit");

    private final BalanceSettings settings;
    /** Mobs waiting for their first balance. Weak, so an unloaded mob is dropped; entities compare by entity id. */
    private final Set<EntityLiving> pending = Collections.newSetFromMap(new WeakHashMap<EntityLiving, Boolean>());
    private final Set<String> reportedFailures = new HashSet<>();

    public MobBalancer(BalanceSettings settings) {
        this.settings = settings;
    }

    /** A mob is any non-player creature. Armor stands and other non-creature living entities are not. */
    public static boolean isMob(Entity entity) {
        return entity instanceof EntityLiving;
    }

    @SubscribeEvent
    public void onAttach(AttachCapabilitiesEvent<Entity> event) {
        BalanceStates.attach(event, MobBalancer::isMob, Contribution::healthFractionForSave);
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity.world.isRemote || !isMob(entity)) {
            return;
        }
        BalanceState state = BalanceStates.get(entity);
        if (state == null) {
            return;
        }
        EntityLiving mob = (EntityLiving) entity;
        if (state.isBalanced()) {
            float saved = state.takeSavedHealthFraction();
            run(mob, state, () -> Contribution.restoreMaxHealth(mob, healthFactor(), saved));
        } else {
            pending.add(mob);
        }
    }

    @SubscribeEvent
    public void onUpdate(LivingEvent.LivingUpdateEvent event) {
        Entity entity = event.getEntityLiving();
        if (entity.world.isRemote || !isMob(entity)) {
            return;
        }
        EntityLiving mob = (EntityLiving) entity;
        boolean first = pending.remove(mob);
        if (!first && (mob.ticksExisted + mob.getEntityId()) % RECHECK_INTERVAL != 0) {
            return;
        }
        BalanceState state = BalanceStates.get(mob);
        if (state == null) {
            return;
        }
        if (first) {
            state.markBalanced();
        } else if (!state.isBalanced()) {
            return;
        } else if (!baseChanged(mob, state)) {
            // Keeps "health rose since the last check" to one recheck interval (see Contribution.fractionToKeep).
            state.setHealthAfterLastApply(mob.getHealth());
            return;
        }
        run(mob, state, () -> Contribution.applyMaxHealth(mob, healthFactor(), state.healthAfterLastApply()));
    }

    private static boolean baseChanged(EntityLiving mob, BalanceState state) {
        return Contribution.base(mob, SharedMonsterAttributes.MAX_HEALTH) != state.lastHealthBase();
    }

    double healthFactor() {
        return settings.mobHealth();
    }

    /** Runs one adjustment and remembers the base and health it left, for the next recheck. */
    private void run(EntityLiving mob, BalanceState state, Step step) {
        try {
            state.setLastHealthBase(Contribution.base(mob, SharedMonsterAttributes.MAX_HEALTH));
            if (!step.apply()) {
                reportOnce(mob, "has no max health attribute; left unchanged", null);
            }
            state.setHealthAfterLastApply(mob.getHealth());
        } catch (RuntimeException e) {
            reportOnce(mob, "could not be balanced; left unchanged", e);
        }
    }

    private void reportOnce(EntityLiving mob, String problem, Throwable cause) {
        String type = mob.getClass().getName();
        if (reportedFailures.add(type + problem)) {
            if (cause == null) {
                LOG.warn("Entity type {} {}", type, problem);
            } else {
                LOG.warn("Entity type {} {}", type, problem, cause);
            }
        }
    }

    private interface Step {
        boolean apply();
    }
}
