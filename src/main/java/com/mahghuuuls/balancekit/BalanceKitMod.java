package com.mahghuuuls.balancekit;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point. Gameplay rules live in their own packages; this class only wires them to the Forge lifecycle.
 * Clients without the mod may join a server that has it, because all balance rules run on the logical server.
 */
@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, acceptableRemoteVersions = "*")
public class BalanceKitMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.debug("{} {} loading", Tags.MOD_NAME, Tags.VERSION);
    }
}
