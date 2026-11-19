package com.mahghuuuls.balancekit.command;

import com.mahghuuuls.balancekit.config.BalanceSettings;
import com.mahghuuuls.balancekit.mob.MobBalancer;
import com.mahghuuuls.balancekit.rules.RuleBook;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the command's own permission check and argument handling with plain senders, as the server would. */
class InspectCommandTest {

    private static ICommandSender sender(int permissionLevel) {
        return new ICommandSender() {
            @Override
            public String getName() {
                return "test";
            }

            @Override
            public boolean canUseCommand(int permLevel, String commandName) {
                return permLevel <= permissionLevel;
            }

            @Override
            public World getEntityWorld() {
                return null;
            }

            @Override
            public MinecraftServer getServer() {
                return null;
            }
        };
    }

    private final InspectCommand command = new InspectCommand(new MobBalancer(new RuleBook(BalanceSettings.NEUTRAL)));

    @Test
    void operatorsMayUseItAndOthersMayNot() {
        assertTrue(command.checkPermission(null, sender(2)));
        assertTrue(command.checkPermission(null, sender(4)));
        assertFalse(command.checkPermission(null, sender(0)));
        assertFalse(command.checkPermission(null, sender(1)));
    }

    @Test
    void aSenderWithoutAnEntityMustGiveATarget() {
        assertThrows(WrongUsageException.class, () -> command.execute(null, sender(4), new String[] {"inspect"}));
    }

    @Test
    void otherSubcommandsAndExtraArgumentsAreUsageErrors() {
        assertThrows(WrongUsageException.class, () -> command.execute(null, sender(4), new String[0]));
        assertThrows(WrongUsageException.class, () -> command.execute(null, sender(4), new String[] {"reload"}));
        assertThrows(WrongUsageException.class,
                () -> command.execute(null, sender(4), new String[] {"inspect", "a", "b"}));
    }
}
