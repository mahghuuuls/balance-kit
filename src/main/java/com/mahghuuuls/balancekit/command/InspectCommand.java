package com.mahghuuuls.balancekit.command;

import com.mahghuuuls.balancekit.damage.DamageScaler;
import com.mahghuuuls.balancekit.mob.MobBalancer;
import com.mahghuuuls.balancekit.player.PlayerBalancer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code /balancekit inspect [target]}: shows an operator what Balance Kit decided for an entity. The target is a
 * player name or an entity selector; without one, an operator player inspects the entity under the crosshair.
 */
public final class InspectCommand extends CommandBase {

    static final double REACH = 16.0D;

    private final MobBalancer mobs;
    private final PlayerBalancer players;
    private final DamageScaler damage;

    public InspectCommand(MobBalancer mobs, PlayerBalancer players, DamageScaler damage) {
        this.mobs = mobs;
        this.players = players;
        this.damage = damage;
    }

    @Override
    public String getName() {
        return "balancekit";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/balancekit inspect [player, or a selector matching one entity]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1 || !"inspect".equals(args[0]) || args.length > 2) {
            throw new WrongUsageException(getUsage(sender));
        }
        Entity target;
        if (args.length == 2) {
            target = getEntity(server, sender, args[1]);
        } else if (sender.getCommandSenderEntity() instanceof EntityPlayerMP) {
            target = lookedAt((EntityPlayerMP) sender.getCommandSenderEntity());
            if (target == null) {
                throw new CommandException("No entity under the crosshair within " + (int) REACH + " blocks.");
            }
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
        for (String line : report(target)) {
            sender.sendMessage(new TextComponentString(line));
        }
    }

    List<String> report(Entity target) {
        ResourceLocation key = EntityList.getKey(target);
        String id = target instanceof EntityPlayer ? "player " + target.getName()
                : key == null ? target.getClass().getName() : key.toString();
        List<String> sections = new ArrayList<>();
        if (MobBalancer.isMob(target)) {
            EntityLiving mob = (EntityLiving) target;
            sections.addAll(ReportFormatter.health(mobs.report(mob)));
            sections.add(ReportFormatter.damage(damage.dealtBy(mob), damage.receivedBy(mob),
                    mobs.profileOf(mob).damageBlocked()));
            sections.addAll(ReportFormatter.entries(mobs.appliedEntries(mob)));
        } else if (PlayerBalancer.isPlayer(target)) {
            sections.add("Player: yes");
            sections.addAll(ReportFormatter.playerHealth(players.report((EntityPlayer) target)));
            sections.add(ReportFormatter.damage(damage.dealtBy(target), damage.receivedBy(target), false));
        } else if (target instanceof EntityPlayer) {
            sections.add("Fake player: Balance Kit gives it no player or mob multipliers.");
        } else {
            sections.add("Not a mob: Balance Kit does not adjust this entity.");
        }
        return ReportFormatter.report(id, target.hasCustomName() ? target.getCustomNameTag() : null,
                target.getEntityId(), sections);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "inspect");
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }

    /**
     * The living entity under the player's crosshair within reach, worked out on the server the way the client's
     * mouse-over does: blocks stop the line of sight, only collidable non-spectating entities count, and an entity
     * whose box contains the eye is the nearest.
     */
    @Nullable
    static Entity lookedAt(EntityPlayerMP player) {
        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        Vec3d end = eye.add(look.scale(REACH));
        RayTraceResult block = player.world.rayTraceBlocks(eye, end, false, false, true);
        double reach = block == null ? REACH : block.hitVec.distanceTo(eye);
        end = eye.add(look.scale(reach));
        AxisAlignedBB area = player.getEntityBoundingBox().expand(look.x * reach, look.y * reach, look.z * reach)
                .grow(1.0D);
        Entity best = null;
        double bestDistance = reach;
        for (Entity candidate : player.world.getEntitiesWithinAABBExcludingEntity(player, area)) {
            if (!(candidate instanceof EntityLivingBase) || !candidate.canBeCollidedWith()
                    || (candidate instanceof EntityPlayer && ((EntityPlayer) candidate).isSpectator())) {
                continue;
            }
            AxisAlignedBB box = candidate.getEntityBoundingBox().grow(candidate.getCollisionBorderSize());
            if (box.contains(eye)) {
                return candidate;
            }
            RayTraceResult hit = box.calculateIntercept(eye, end);
            if (hit != null) {
                double distance = eye.distanceTo(hit.hitVec);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
        }
        return best;
    }
}
